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
    private final InterviewAnswerAnalysisJobService interviewAnswerAnalysisJobService;

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
         * 핵심 변경점.
         *
         * questionId는 재사용 가능한 QuestionEntity 원본 ID이다.
         * 따라서 QuestionRepository에서 질문만 단독 조회하지 않고,
         * 반드시 현재 interviewId와 questionId가 함께 존재하는
         * interview_questions 매핑을 조회한다.
         *
         * 이 검증으로 다른 면접에서 사용한 질문을 잘못 연결하는 것도 막는다.
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
                        // 기존 분석/조회 코드 호환용. 기준 관계는 interviewQuestion.
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
         * 2단계: 질문별 답변은 "연결 + 저장"까지만 한다.
         *
         * questionId != null
         *   -> InterviewQuestion과 InterviewAnswer 연결
         *   -> MinIO key 저장
         *   -> 분석은 3단계에서 별도 비동기 Speech/Gemini Job으로 연결
         *
         * questionId == null
         *   -> 기존 전체 면접 영상
         *   -> eye / pose / speech 전체 분석 흐름은 그대로 유지
         */
        if (questionId == null) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            interviewAnswerAnalysisJobService
                                    .analyzeAnswerAsync(
                                            answerId,
                                            videoObjectKey
                                    );
                        }
                    }
            );
        }

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
