package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerAnalysisJobService {

    private final InterviewAnswerRepository interviewAnswerRepository;
    private final EyeBlinkingService eyeBlinkingService;
    private final PoseService poseService;
    private final SpeechAnalysisService speechAnalysisService;
    private final TransactionTemplate transactionTemplate;

    /*
     * 답변 영상 분석
     *
     * - eye blinking
     * - pose
     * - speech / STT
     */
    @Async("analysisExecutor")
    public void analyzeAnswerAsync(
            Long answerId,
            String videoObjectKey
    ) {

        log.info(
                "답변 분석 시작: answerId={}, videoObjectKey={}",
                answerId,
                videoObjectKey
        );

        InterviewAnswerEntity answer =
                markRunning(answerId);

        try {

            InterviewEntity interview =
                    answer.getInterviewSafely();

            if (interview == null) {

                throw new IllegalStateException(
                        "답변에 연결된 면접 정보를 찾을 수 없습니다. "
                                + "answerId="
                                + answerId
                );
            }

            /*
             * 1. 눈 깜빡임 분석
             */
            eyeBlinkingService.analyzeAndSave(
                    videoObjectKey,
                    interview
            );

            /*
             * 2. 자세 분석
             */
            poseService.processPostureAnalysis(
                    videoObjectKey,
                    interview
            );

            /*
             * 3. Speech / STT 분석
             */
            analyzeSpeechAndSaveAnswerText(
                    answerId,
                    videoObjectKey,
                    interview,
                    answer
            );

            /*
             * 모든 분석 완료
             */
            markDone(answerId);

            log.info(
                    "답변 분석 완료: answerId={}",
                    answerId
            );

        } catch (Exception e) {

            log.error(
                    "답변 분석 실패: answerId={}, error={}",
                    answerId,
                    e.getMessage(),
                    e
            );

            markFailed(
                    answerId,
                    e.getMessage()
            );
        }
    }

    /*
     * FastAPI Speech 분석 실행 후
     * STT 결과를 InterviewAnswer.answerText에 저장
     */
    private void analyzeSpeechAndSaveAnswerText(
            Long answerId,
            String videoObjectKey,
            InterviewEntity interview,
            InterviewAnswerEntity answer
    ) {

        String jobCategory = "";

        if (interview.getJobRole() != null) {

            if (
                    interview
                            .getJobRole()
                            .getCategory() != null
            ) {

                jobCategory =
                        interview
                                .getJobRole()
                                .getCategory();
            }
        }

        Long questionId = null;

        /*
         * 일반 면접 질문
         */
        if (answer.getQuestion() != null) {

            questionId =
                    answer
                            .getQuestion()
                            .getId();
        }

        /*
         * PT 질문
         */
        if (
                questionId == null
                        && answer.getPtQuestion() != null
        ) {

            questionId =
                    answer
                            .getPtQuestion()
                            .getId();
        }

        String originalQuestion =
                answer.getQuestionText();

        /*
         * FastAPI:
         * POST /apis/speech/analyze
         */
        Map<String, Object> speechResponse =
                speechAnalysisService.analyzeSpeech(
                        "minio",
                        videoObjectKey,
                        jobCategory,
                        interview.getId(),
                        answerId,
                        questionId,
                        originalQuestion
                );

        String textScript =
                extractTextScript(
                        speechResponse
                );

        if (
                textScript == null
                        || textScript.isBlank()
        ) {

            log.warn(
                    "Speech 분석은 완료됐지만 STT 결과가 비어 있습니다. "
                            + "answerId={}",
                    answerId
            );

            return;
        }

        saveAnswerText(
                answerId,
                textScript
        );

        log.info(
                "답변 STT 저장 완료: answerId={}, textLength={}",
                answerId,
                textScript.length()
        );
    }

    /*
     * FastAPI 응답에서 text_script 추출
     *
     * 예상 구조:
     *
     * {
     *   "result": {
     *       "text_script": "..."
     *   }
     * }
     *
     * 혹시 최상위에 text_script가 오는 경우도
     * 같이 처리한다.
     */
    @SuppressWarnings("unchecked")
    private String extractTextScript(
            Map<String, Object> response
    ) {

        if (response == null) {
            return null;
        }

        /*
         * 1. result.text_script
         */
        Object resultObject =
                response.get("result");

        if (resultObject instanceof Map<?, ?>) {

            Map<String, Object> result =
                    (Map<String, Object>) resultObject;

            Object textScript =
                    result.get("text_script");

            if (textScript != null) {

                return String.valueOf(
                        textScript
                ).trim();
            }

            /*
             * 혹시 transcript라는 이름으로
             * 반환되는 경우도 대응
             */
            Object transcript =
                    result.get("transcript");

            if (transcript != null) {

                return String.valueOf(
                        transcript
                ).trim();
            }
        }

        /*
         * 2. 최상위 text_script
         */
        Object textScript =
                response.get("text_script");

        if (textScript != null) {

            return String.valueOf(
                    textScript
            ).trim();
        }

        /*
         * 3. 최상위 transcript
         */
        Object transcript =
                response.get("transcript");

        if (transcript != null) {

            return String.valueOf(
                    transcript
            ).trim();
        }

        return null;
    }

    /*
     * STT 결과 저장
     */
    private void saveAnswerText(
            Long answerId,
            String answerText
    ) {

        transactionTemplate.executeWithoutResult(
                status -> {

                    InterviewAnswerEntity answer =
                            interviewAnswerRepository
                                    .findById(
                                            answerId
                                    )
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. "
                                                                    + "answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.updateAnswerText(
                            answerText
                    );

                    interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }

    private InterviewAnswerEntity markRunning(
            Long answerId
    ) {

        return transactionTemplate.execute(
                status -> {

                    InterviewAnswerEntity answer =
                            interviewAnswerRepository
                                    .findWithInterviewById(
                                            answerId
                                    )
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. "
                                                                    + "answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.markRunning();

                    return interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }

    private void markDone(
            Long answerId
    ) {

        transactionTemplate.executeWithoutResult(
                status -> {

                    InterviewAnswerEntity answer =
                            interviewAnswerRepository
                                    .findById(
                                            answerId
                                    )
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. "
                                                                    + "answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.markDone();

                    interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }

    private void markFailed(
            Long answerId,
            String errorMessage
    ) {

        transactionTemplate.executeWithoutResult(
                status -> {

                    InterviewAnswerEntity answer =
                            interviewAnswerRepository
                                    .findById(
                                            answerId
                                    )
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. "
                                                                    + "answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.markFailed(
                            errorMessage
                    );

                    interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }
}