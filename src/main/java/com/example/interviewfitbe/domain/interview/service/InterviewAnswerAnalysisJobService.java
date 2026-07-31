package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerAnalysisJobService {

    private final InterviewAnswerRepository interviewAnswerRepository;
    private final EyeBlinkingService eyeBlinkingService;
    private final TransactionTemplate transactionTemplate;

    /*
     * 구현완료:
     * - eye blinking
     *
     * 구현 필요:
     * - eye gaze
     * - pose
     * - speech
     *
     */
    @Async("analysisExecutor")
    public void analyzeAnswerAsync(Long answerId, String videoObjectKey) {
        log.info("답변 분석 시작: answerId={}, videoObjectKey={}", answerId, videoObjectKey);

        InterviewAnswerEntity answer = markRunning(answerId);

        try {
            InterviewEntity interview = answer.getInterviewSafely();

            if (interview == null) {
                throw new IllegalStateException(
                        "답변에 연결된 면접 정보를 찾을 수 없습니다. answerId=" + answerId
                );
            }

            /*
             * Spring → FastAPI eye blinking 호출
             * FastAPI: POST /apis/eye-blinking/analyze
             */
            eyeBlinkingService.analyzeAndSave(videoObjectKey, interview);

            markDone(answerId);

            log.info("답변 분석 완료: answerId={}", answerId);

        } catch (Exception e) {
            log.error("답변 분석 실패: answerId={}, error={}", answerId, e.getMessage(), e);
            markFailed(answerId, e.getMessage());
        }
    }

    private InterviewAnswerEntity markRunning(Long answerId) {
        return transactionTemplate.execute(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findWithInterviewById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markRunning();

            return interviewAnswerRepository.save(answer);
        });
    }

    private void markDone(Long answerId) {
        transactionTemplate.executeWithoutResult(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markDone();
            interviewAnswerRepository.save(answer);
        });
    }

    private void markFailed(Long answerId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markFailed(errorMessage);
            interviewAnswerRepository.save(answer);
        });
    }
}