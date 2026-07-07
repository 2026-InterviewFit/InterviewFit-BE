package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
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
    private final VideoService videoService;
    private final InterviewAnswerAnalysisJobService interviewAnswerAnalysisJobService;

    /**
     * 질문별 답변 영상 업로드.
     *
     * 흐름:
     * 1. interviewId로 면접 세션 조회
     * 2. 영상 파일을 MinIO에 저장
     * 3. interview_answer 테이블에 답변 row 저장
     * 4. DB commit 이후 비동기 분석 시작
     * 5. answerId와 videoObjectKey 반환
     */
    @Transactional
    public InterviewAnswerUploadResponse uploadAnswerVideo(
            Long interviewId,
            MultipartFile file,
            Long questionId,
            String questionText,
            Integer answerOrder
    ) throws IOException {
        if (answerOrder == null || answerOrder <= 0) {
            throw new IllegalArgumentException("answerOrder는 1 이상이어야 합니다.");
        }

        InterviewEntity interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + interviewId
                ));

        String videoObjectKey = videoService.uploadVideoToMinio(file);

        InterviewAnswerEntity answer = InterviewAnswerEntity.builder()
                .interview(interview)
                .questionId(questionId)
                .questionText(questionText)
                .videoObjectKey(videoObjectKey)
                .answerOrder(answerOrder)
                .analysisStatus(AnalysisStatus.UPLOADED)
                .build();

        InterviewAnswerEntity saved = interviewAnswerRepository.save(answer);

        Long answerId = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                interviewAnswerAnalysisJobService.analyzeAnswerAsync(answerId);
            }
        });

        return InterviewAnswerUploadResponse.builder()
                .answerId(saved.getId())
                .interviewId(interview.getId())
                .questionId(saved.getQuestionId())
                .questionText(saved.getQuestionText())
                .videoObjectKey(saved.getVideoObjectKey())
                .answerOrder(saved.getAnswerOrder())
                .analysisStatus(saved.getAnalysisStatus())
                .build();
    }
}
