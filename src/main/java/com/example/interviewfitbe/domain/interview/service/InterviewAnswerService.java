package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.entity.VideoType;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
            Long interviewId,
            MultipartFile file,
            Long questionId,
            Integer answerOrder,
            Double startTime,
            Double endTime
    ) throws IOException {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId는 필수입니다.");
        }

        if (questionId == null) {
            throw new IllegalArgumentException("questionId는 필수입니다.");
        }

        if (answerOrder == null || answerOrder <= 0) {
            throw new IllegalArgumentException("answerOrder는 1 이상이어야 합니다.");
        }

        InterviewEntity interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + interviewId
                ));

        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 질문을 찾을 수 없습니다. questionId=" + questionId
                ));

        /*
         * 배예은 VideoService를 사용해서
         * MinIO 업로드 + videos 테이블 저장까지 같이 처리.
         */
        VideoUrlResponse videoResponse = videoService.saveVideoFile(
                interviewId,
                file,
                VideoType.INTERVIEW_FULL
        );

        String videoObjectKey = videoResponse.getVideoUrl();

        InterviewAnswerEntity answer = InterviewAnswerEntity.builder()
                .interview(interview)
                .question(question)
                .answerOrder(answerOrder)
                .startTime(startTime)
                .endTime(endTime)
                .analysisStatus(AnalysisStatus.UPLOADED)
                .build();

        InterviewAnswerEntity saved = interviewAnswerRepository.save(answer);
        Long answerId = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                interviewAnswerAnalysisJobService.analyzeAnswerAsync(answerId, videoObjectKey);
            }
        });

        return InterviewAnswerUploadResponse.builder()
                .answerId(saved.getId())
                .interviewId(interview.getId())
                .questionId(question.getId())
                .answerOrder(saved.getAnswerOrder())
                .videoObjectKey(videoObjectKey)
                .analysisStatus(saved.getAnalysisStatus())
                .build();
    }
}