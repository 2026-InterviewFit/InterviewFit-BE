package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewQuestionEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewQuestionRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class InterviewAnswerService {

    private final InterviewRepository interviewRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final VideoService videoService;

    /*
     * 전체 면접 영상 분석 전담.
     * eye / pose / speech / Gemini 전체 분석을 수행한다.
     */
    private final InterviewAnswerAnalysisJobService interviewAnswerAnalysisJobService;

    /*
     * 일반 질문별 답변 분석 전담.
     * audio/webm 기준 STT + Gemini 내용 평가만 비동기로 수행한다.
     */
    private final QuestionAnswerAnalysisJobService questionAnswerAnalysisJobService;

    @Transactional
    public InterviewAnswerUploadResponse uploadAnswerVideo(
            Long userId,
            Long interviewId,
            MultipartFile file,
            Long questionId,
            Integer answerOrder,
            Double startTime,
            Double endTime
    ) throws IOException {

        if (answerOrder == null || answerOrder <= 0) {
            throw new IllegalArgumentException(
                    "answerOrder는 1 이상이어야 합니다."
            );
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "업로드할 영상 파일이 없습니다."
            );
        }

        InterviewEntity interview =
                interviewRepository.findById(interviewId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "해당 면접을 찾을 수 없습니다. interviewId="
                                                + interviewId
                                )
                        );

        Long ownerUserId = interview.getUser().getId();

        System.out.println("======================================");
        System.out.println("[답변 업로드 권한 확인]");
        System.out.println("interviewId = " + interviewId);
        System.out.println("JWT userId = " + userId);
        System.out.println("면접 ownerUserId = " + ownerUserId);
        System.out.println("questionId = " + questionId);
        System.out.println("answerOrder = " + answerOrder);
        System.out.println("======================================");

        if (!ownerUserId.equals(userId)) {
            throw new AccessDeniedException(
                    "본인의 면접에만 답변을 업로드할 수 있습니다."
            );
        }

        /*
         * questionId는 재사용 가능한 QuestionEntity 원본 ID이다.
         * 반드시 현재 interviewId + questionId 조합의
         * InterviewQuestionEntity를 통해 이번 면접의 질문인지 검증한다.
         */
        InterviewQuestionEntity interviewQuestion = null;
        QuestionEntity question = null;

        if (questionId != null) {

            interviewQuestion =
                    interviewQuestionRepository
                            .findByInterview_IdAndQuestion_Id(
                                    interviewId,
                                    questionId
                            )
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "해당 면접에 연결된 질문을 찾을 수 없습니다. "
                                                    + "interviewId="
                                                    + interviewId
                                                    + ", questionId="
                                                    + questionId
                                    )
                            );

            question = interviewQuestion.getQuestion();
        }

        String videoObjectKey =
                videoService.uploadVideoToMinio(file);

        InterviewAnswerEntity answer =
                InterviewAnswerEntity.builder()
                        .interview(interview)
                        .interviewQuestion(interviewQuestion)
                        .question(question)
                        .answerOrder(answerOrder)
                        .startTime(startTime)
                        .endTime(endTime)
                        .videoObjectKey(videoObjectKey)
                        .analysisStatus(AnalysisStatus.UPLOADED)
                        .build();

        InterviewAnswerEntity saved =
                interviewAnswerRepository.save(answer);

        Long answerId = saved.getId();

        /*
         * 트랜잭션 커밋 후 분석을 시작한다.
         *
         * questionId != null
         *   -> 질문별 audio 답변
         *   -> STT + Gemini 내용 평가 비동기 실행
         *   -> InterviewAnswer.answerText + AiEvaluation 저장
         *
         * questionId == null
         *   -> 전체 면접 video
         *   -> 기존 eye / pose / speech / Gemini 전체 분석 비동기 실행
         */
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        if (questionId != null) {
                            questionAnswerAnalysisJobService
                                    .analyzeQuestionAnswerAsync(
                                            answerId,
                                            videoObjectKey
                                    );
                            return;
                        }

                        interviewAnswerAnalysisJobService
                                .analyzeAnswerAsync(
                                        answerId,
                                        videoObjectKey
                                );
                    }
                }
        );

        return InterviewAnswerUploadResponse.builder()
                .answerId(saved.getId())
                .interviewId(interview.getId())
                .interviewQuestionId(
                        interviewQuestion == null
                                ? null
                                : interviewQuestion.getId()
                )
                .questionId(
                        question == null
                                ? null
                                : question.getId()
                )
                .answerOrder(saved.getAnswerOrder())
                .videoObjectKey(videoObjectKey)
                .analysisStatus(saved.getAnalysisStatus())
                .build();
    }
}
