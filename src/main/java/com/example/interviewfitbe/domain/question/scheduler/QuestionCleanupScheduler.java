package com.example.interviewfitbe.domain.question.scheduler;

import com.example.interviewfitbe.domain.question.enums.QuestionType;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class QuestionCleanupScheduler {
    private static final int DELETE_LIMIT = 1000;
    private static final int DEFAULT_QUESTION_EXPIRATION_HOURS = 1;

    private final QuestionRepository questionRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredDefaultQuestions() {
        int deletedCount = 0;
        LocalDateTime expiredAt = LocalDateTime.now().minusHours(DEFAULT_QUESTION_EXPIRATION_HOURS);

        try {
            List<Long> ids = questionRepository.findExpiredQuestionIds(
                    QuestionType.DEFAULT,
                    expiredAt,
                    PageRequest.of(0, DELETE_LIMIT)
            );

            if (!ids.isEmpty()) {
                deletedCount = questionRepository.deleteByIds(ids);
            }

            log.info(
                    "[QuestionCleanupScheduler] Deleted {} expired default questions. expiredAt={}",
                    deletedCount,
                    expiredAt
            );
        } catch (Exception e) {
            log.error(
                    "Failed to delete expired default questions. expiredAt={}",
                    expiredAt,
                    e
            );
        }
    }
}
