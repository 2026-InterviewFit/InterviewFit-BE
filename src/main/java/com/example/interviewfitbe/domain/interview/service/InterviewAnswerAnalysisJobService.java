package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechReportService;
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
    private final PoseService poseService;
    private final SpeechReportService speechReportService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 질문별 답변 영상 분석 Job.
     *
     * 흐름:
     * 1. interview_answer 상태를 RUNNING으로 변경
     * 2. eye 분석 실행 및 저장
     * 3. pose 분석 실행 및 저장
     * 4. speech 분석 실행 및 저장
     * 5. 성공 시 DONE
     * 6. 실패 시 FAILED
     */
    @Async("analysisExecutor")
    public void analyzeAnswerAsync(Long answerId) {
        log.info("질문별 답변 분석 시작: answerId={}", answerId);

        InterviewAnswerEntity answer = markRunning(answerId);

        try {
            String videoObjectKey = answer.getVideoObjectKey();

            eyeBlinkingService.analyzeAndSave(videoObjectKey, answer);

            poseService.analyzeFullPosture(videoObjectKey, answer);

            speechReportService.analyzeAndSave(answer);

            markDone(answerId);

            log.info("질문별 답변 분석 완료: answerId={}", answerId);

        } catch (Exception e) {
            log.error("질문별 답변 분석 실패: answerId={}, error={}", answerId, e.getMessage(), e);
            markFailed(answerId, e.getMessage());
        }
    }

    private InterviewAnswerEntity markRunning(Long answerId) {
        return transactionTemplate.execute(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findWithInterviewById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변 영상을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markRunning();

            return interviewAnswerRepository.save(answer);
        });
    }

    private void markDone(Long answerId) {
        transactionTemplate.executeWithoutResult(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변 영상을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markDone();

            interviewAnswerRepository.save(answer);
        });
    }

    private void markFailed(Long answerId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변 영상을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markFailed(errorMessage);

            interviewAnswerRepository.save(answer);
        });
    }
}