package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
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
    private final QuestionRepository questionRepository;
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

        Long ownerUserId =
                interview.getUser().getId();

        System.out.println("======================================");
        System.out.println("[답변 업로드 권한 확인]");
        System.out.println("interviewId = " + interviewId);
        System.out.println("JWT userId = " + userId);
        System.out.println("면접 ownerUserId = " + ownerUserId);
        System.out.println("questionId = " + questionId);
        System.out.println("answerOrder = " + answerOrder);
        System.out.println("======================================");

        /*
         * 현재 로그인한 사용자가 생성한 면접인지 확인한다.
         */
        if (!ownerUserId.equals(userId)) {
            throw new AccessDeniedException(
                    "본인의 면접에만 답변을 업로드할 수 있습니다."
            );
        }

        /*
         * questionId가 전달된 경우에만 질문을 조회한다.
         *
         * 현재 프론트처럼 전체 면접 영상을 한 번에 업로드하면
         * questionId는 null이어도 된다.
         */
        QuestionEntity question = null;

        if (questionId != null) {
            question =
                    questionRepository.findById(questionId)
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "해당 질문을 찾을 수 없습니다. questionId="
                                                    + questionId
                                    )
                            );
        }

        String videoObjectKey =
                videoService.uploadVideoToMinio(file);

        InterviewAnswerEntity answer =
                InterviewAnswerEntity.builder()
                        .interview(interview)
                        .question(question)
                        .answerOrder(answerOrder)
                        .startTime(startTime)
                        .endTime(endTime)
                        .analysisStatus(AnalysisStatus.UPLOADED)
                        .build();

        InterviewAnswerEntity saved =
                interviewAnswerRepository.save(answer);

        Long answerId = saved.getId();

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

        return InterviewAnswerUploadResponse.builder()
                .answerId(saved.getId())
                .interviewId(interview.getId())
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