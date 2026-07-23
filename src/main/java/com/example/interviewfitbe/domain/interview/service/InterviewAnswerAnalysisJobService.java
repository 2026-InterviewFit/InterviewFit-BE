package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.eye_gaze.service.EyeGazeService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.service.PoseService;
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
    private final EyeGazeService eyeGazeService;
    private final PoseService poseService;
    private final TransactionTemplate transactionTemplate;

    /*
     * 답변 영상 분석 Job.
     *
     * 현재 speech는 팀원 리팩토링 결과가 interview 단위로 바뀌어 있음.
     * 그래서 여기서 answer 단위로 바로 호출하지 않는다.
     *
     * 여기서는 현재 컴파일 가능한 eye-blink, eye-gaze, pose만 호출한다.
     */
    @Async("analysisExecutor")
    public void analyzeAnswerAsync(Long answerId, String videoObjectKey) {
        log.info("답변 분석 시작: answerId={}", answerId);

        InterviewAnswerEntity answer = markRunning(answerId);

        try {
            InterviewEntity interview = answer.getInterviewSafely();

            if (interview == null) {
                throw new IllegalStateException(
                        "답변에 연결된 면접 정보를 찾을 수 없습니다. answerId=" + answerId
                );
            }

            /*
             * 사용자 담당: eye blinking
             */
            eyeBlinkingService.analyzeAndSave(videoObjectKey, interview);

            /*
             * 팀원 담당이지만 현재 서비스 시그니처가 interview 기준으로 정리되어 있으므로
             * InterviewEntity를 넘긴다.
             */
            eyeGazeService.analyzeAndSave(videoObjectKey, interview);
            poseService.processPostureAnalysis(videoObjectKey, interview);

            /*
             * speech는 현재 SpeechReportEntity가 interview_id unique 구조라
             * answer마다 호출하면 중복 저장 오류가 날 수 있음.
             * speech 팀원이 전체 면접 영상 기준 호출 위치를 정하면 다시 연결.
             */

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