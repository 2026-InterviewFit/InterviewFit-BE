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

    /**
     * 질문별 답변 영상 업로드.
     *
     * 흐름:
     * 1. interviewId로 면접 세션 조회
     * 2. questionId로 QuestionEntity 조회 (필수)
     * 3. 영상 파일을 MinIO에 저장
     * 4. interview_answers 테이블에 답변 row 저장
     * 5. DB commit 이후 비동기 분석 시작 (videoObjectKey를 파라미터로 전달)
     * 6. answerId와 videoObjectKey 반환
     */
    @Transactional
    public InterviewAnswerUploadResponse uploadAnswerVideo(
            Long interviewId,
            MultipartFile file,
            Long questionId,
            Integer answerOrder,
            Double startTime,
            Double endTime
    ) throws IOException {
        if (answerOrder == null || answerOrder <= 0) {
            throw new IllegalArgumentException("answerOrder는 1 이상이어야 합니다.");
        }

        if (questionId == null) {
            throw new IllegalArgumentException("questionId는 필수입니다.");
        }

        InterviewEntity interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + interviewId
                ));

        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 질문을 찾을 수 없습니다. questionId=" + questionId
                ));

        // MinIO에 영상 저장
        String videoObjectKey = videoService.uploadVideoToMinio(file);

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

        // 트랜잭션 커밋 후 비동기 분석 시작
        // videoObjectKey는 InterviewAnswerEntity에 저장하지 않고 파라미터로 넘김
        // TODO: 배예은의 VideoEntity 완성 후 VideoEntity FK로 연결 예정
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