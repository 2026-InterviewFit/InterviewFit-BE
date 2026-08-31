package com.example.interviewfitbe.domain.group.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupInterviewAnswerTimeoutScheduler {

    private final GroupInterviewFlowService flowService;

    @Scheduled(fixedDelayString = "${app.group-interview.timeout-check-ms:1000}")
    public void completeExpiredAnswers() {
        Instant now = Instant.now();
        for (Long sessionId : flowService.findExpiredAnswerStartSessionIds(now)) {
            try {
                flowService.skipExpiredAnswerStart(sessionId);
            } catch (RuntimeException exception) {
                log.warn("답변 시작 대기 만료 처리 실패. sessionId={}", sessionId, exception);
            }
        }
        for (Long sessionId : flowService.findExpiredAnswerSessionIds(now)) {
            try {
                flowService.completeExpiredAnswer(sessionId);
            } catch (RuntimeException exception) {
                // 사용자 완료 요청과 경합한 경우 다음 조회부터 자연스럽게 제외된다.
                log.warn("만료된 다대일 면접 답변 처리 실패. sessionId={}", sessionId, exception);
            }
        }
    }
}
