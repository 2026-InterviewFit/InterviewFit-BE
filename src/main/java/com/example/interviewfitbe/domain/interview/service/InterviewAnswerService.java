package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
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
    private final InterviewAnswerAnalysisJobService interviewAnswerAnalysisJobService;

    /*
     * 현재 메서드명은 uploadAnswerVideo지만,
     * 최종 구조에서는 답변별 영상을 저장하지 않고
     * question + timestamp 기반 answer row만 저장한다.
     *
     * file 파라미터는 기존 컨트롤러 호환 때문에 남겨둠.
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

        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 질문을 찾을 수 없습니다. questionId=" + questionId
                ));

        InterviewAnswerEntity answer = InterviewAnswerEntity.builder()
                .interview(interview)
                .question(question)
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
                .videoObjectKey(null)
                .answerOrder(saved.getAnswerOrder())
                .analysisStatus(saved.getAnalysisStatus())
                .build();
    }
}