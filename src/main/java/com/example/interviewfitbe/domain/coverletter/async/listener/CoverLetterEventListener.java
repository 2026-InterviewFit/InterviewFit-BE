package com.example.interviewfitbe.domain.coverletter.async.listener;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.async.event.CoverLetterCreatedEvent;
import com.example.interviewfitbe.domain.coverletter.service.embedding.CoverLetterQuestionEmbeddingService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CoverLetterEventListener {
    private final CoverLetterQuestionService coverLetterQuestionService;
    private final CoverLetterQuestionEmbeddingService embeddingService;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CoverLetterCreatedEvent event) {
        // 자기소개서 항목별 저장 및 항목별 분석 결과 저장
        List<CoverLetterQuestionEntity> questions = coverLetterQuestionService.saveCoverLetterQuestions(event.getAnalyses(), event.getCoverLetter());

        // 자기소개서 항목별 임베딩 저장
        embeddingService.createEmbedding(questions);
    }
}
