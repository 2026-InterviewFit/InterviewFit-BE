package com.example.interviewfitbe.domain.coverletter.async.listener;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterProcessingEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.async.event.CoverLetterCreatedEvent;
import com.example.interviewfitbe.domain.coverletter.service.processing.CoverLetterProcessingService;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.coverletter.service.embedding.CoverLetterQuestionEmbeddingService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CoverLetterEventListener {
    private final CoverLetterService coverLetterService;
    private final CoverLetterQuestionService coverLetterQuestionService;
    private final CoverLetterQuestionEmbeddingService embeddingService;
    private final CoverLetterProcessingService processingService;

    @Async("coverLetterExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handle(CoverLetterCreatedEvent event) {
        long start = System.currentTimeMillis();

        CoverLetterEntity coverLetter = coverLetterService.getCoverLetter(event.getCoverLetterId());
        CoverLetterProcessingEntity processing = processingService.getProcessingByCoverLetter(coverLetter);

        try {
            processing.processing();

            // 자기소개서 항목별 저장 및 항목별 분석 결과 저장
            List<CoverLetterQuestionEntity> questions = coverLetterQuestionService.saveCoverLetterQuestions(event.getAnalyses(), coverLetter);
            log.info("saveQuestions async: {}ms", System.currentTimeMillis() - start);

            // 자기소개서 항목별 임베딩 저장
            embeddingService.createEmbedding(questions);
            log.info("embedding async: {}ms", System.currentTimeMillis() - start);

            processing.completed();
        } catch (Exception e) {
            processing.failed();
            throw new RuntimeException(e);
        }
    }
}
