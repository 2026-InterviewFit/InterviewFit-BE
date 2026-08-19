package com.example.interviewfitbe.domain.coverletter.async.listener;

import com.example.interviewfitbe.domain.coverletter.async.event.CoverLetterEmbeddingRetryEvent;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterProcessingEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.async.event.CoverLetterCreatedEvent;
import com.example.interviewfitbe.domain.coverletter.service.processing.CoverLetterProcessingService;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.coverletter.service.embedding.CoverLetterQuestionEmbeddingService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionService;
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
    public void handle(CoverLetterCreatedEvent event) {
        long start = System.currentTimeMillis();

        CoverLetterEntity coverLetter = coverLetterService.getCoverLetter(event.getCoverLetterId());
        CoverLetterProcessingEntity processing = processingService.getProcessingByCoverLetter(coverLetter);

        try {
            processingService.processing(processing);

            // 자기소개서 항목별 저장 및 항목별 분석 결과 저장
            List<CoverLetterQuestionEntity> questions = coverLetterQuestionService.saveCoverLetterQuestions(event.getAnalyses(), coverLetter);
            log.info("saveQuestions async: {}ms", System.currentTimeMillis() - start);

            // 자기소개서 항목별 임베딩 저장
            embeddingService.createEmbedding(questions);
            log.info("embedding async: {}ms", System.currentTimeMillis() - start);

            processingService.complete(processing);
        } catch (Exception e) {
            processingService.fail(processing);
            throw new RuntimeException(e);
        }
    }

    @Async("coverLetterExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmbeddingRetry(CoverLetterEmbeddingRetryEvent event) {
        CoverLetterEntity coverLetter = coverLetterService.getCoverLetter(event.getCoverLetterId());
        CoverLetterProcessingEntity processing = processingService.getProcessingByCoverLetter(coverLetter);

        if (processing.isCompleted()) {
            return;
        }

        try {
            // 임베딩 없는 자소서 문항 조회
            List<CoverLetterQuestionEntity> questions = coverLetterQuestionService.getQuestionsWithoutEmbedding(coverLetter.getId());

            // 임베딩 생성 및 저장
            embeddingService.createEmbedding(questions);

            processingService.complete(processing);
        } catch (Exception e) {
            processingService.fail(processing);
            throw new RuntimeException(e);
        }
    }
}
