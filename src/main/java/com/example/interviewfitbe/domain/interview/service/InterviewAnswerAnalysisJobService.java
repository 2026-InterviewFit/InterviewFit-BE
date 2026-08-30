package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
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
    private final AiEvaluationRepository aiEvaluationRepository;
    private final TransactionTemplate transactionTemplate;

    /*
     * 답변 영상 분석
     *
     * - eye blinking
     * - pose
     * - speech / STT / Gemini
     *
     * 중요:
     * SpeechAnalysisService를 직접 호출하지 않는다.
     * 반드시 SpeechReportService.analyzeAndSave()를 통해
     * speech_report + ai_evaluation까지 저장한 뒤 DONE 처리한다.
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
                        "답변에 연결된 면접 정보를 찾을 수 없습니다. answerId="
                                + answerId
                );
            }

            /*
             * 1. 눈 깜빡임 분석
             */
            log.info("눈 깜빡임 분석 시작: answerId={}", answerId);

            eyeBlinkingService.analyzeAndSave(
                    videoObjectKey,
                    interview
            );

            log.info("눈 깜빡임 분석 완료: answerId={}", answerId);

            /*
             * 2. 자세 분석
             */
            log.info("자세 분석 시작: answerId={}", answerId);

            poseService.processPostureAnalysis(
                    videoObjectKey,
                    interview
            );

            log.info("자세 분석 완료: answerId={}", answerId);

            /*
             * 3. Speech / STT / Gemini 분석 + DB 저장
             */
            analyzeSpeechGeminiAndSave(
                    answerId,
                    videoObjectKey,
                    interview,
                    answer
            );

            /*
             * Gemini 평가가 실제 DB에 저장됐는지 마지막 검증.
             *
             * 이전 코드에서는 FastAPI 호출만 성공하면
             * ai_evaluation이 없어도 DONE으로 바뀔 수 있었기 때문에
             * 저장 결과를 확인한 뒤 DONE 처리한다.
             */
            if (!aiEvaluationRepository
                    .findByInterviewAnswer_Id(answerId)
                    .isPresent()) {

                throw new IllegalStateException(
                        "Gemini AI 평가 결과가 저장되지 않았습니다. answerId="
                                + answerId
                );
            }

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
     * Speech / STT / Gemini 분석 및 저장
     *
     * 호출 흐름:
     * InterviewAnswerAnalysisJobService
     *      -> SpeechReportService.analyzeAndSave()
     *      -> SpeechAnalysisService.analyzeSpeech()
     *      -> FastAPI /apis/speech/analyze
     *      -> speech_report 저장
     *      -> ai_evaluation 저장
     *
     * 이 메서드에서는 FastAPI를 별도로 다시 호출하지 않는다.
     */
    private void analyzeSpeechGeminiAndSave(
            Long answerId,
            String videoObjectKey,
            InterviewEntity interview,
            InterviewAnswerEntity answer
    ) {

        String jobCategory = "";

        if (
                interview.getJobRole() != null
                        && interview.getJobRole().getCategory() != null
        ) {
            jobCategory =
                    interview.getJobRole().getCategory();
        }

        Long questionId = null;

        /*
         * 일반 면접 질문
         */
        if (answer.getQuestion() != null) {
            questionId =
                    answer.getQuestion().getId();
        }

        /*
         * PT 질문
         */
        if (
                questionId == null
                        && answer.getPtQuestion() != null
        ) {
            questionId =
                    answer.getPtQuestion().getId();
        }

        String originalQuestion =
                answer.getQuestionText();

        SpeechAnalyzeRequest request =
                SpeechAnalyzeRequest.builder()
                        .sourceType("minio")
                        .sourceValue(videoObjectKey)
                        .jobCategory(jobCategory)
                        .interviewId(interview.getId())
                        .interviewAnswerId(answerId)
                        .questionId(questionId)
                        .originalQuestion(originalQuestion)
                        .build();

        log.info(
                "Speech/Gemini 분석 시작: answerId={}, interviewId={}",
                answerId,
                interview.getId()
        );

        /*
         * FastAPI 호출은 이 한 번뿐이다.
         * SpeechReportService 내부에서 결과까지 DB에 저장한다.
         */
        Long speechReportId =
                speechReportService.analyzeAndSave(request);

        SpeechReportEntity speechReport =
                speechReportService.getReport(speechReportId);

        String textScript =
                speechReport.getTextScript();

        if (
                textScript != null
                        && !textScript.isBlank()
        ) {
            saveAnswerText(
                    answerId,
                    textScript
            );

            log.info(
                    "답변 STT 저장 완료: answerId={}, textLength={}",
                    answerId,
                    textScript.length()
            );
        } else {
            log.warn(
                    "Speech 분석은 완료됐지만 STT 결과가 비어 있습니다. answerId={}",
                    answerId
            );
        }

        log.info(
                "Speech/Gemini 분석 완료: answerId={}, speechReportId={}",
                answerId,
                speechReportId
        );
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
                                    .findById(answerId)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.updateAnswerText(answerText);

                    interviewAnswerRepository.save(answer);
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
                                    .findWithInterviewById(answerId)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.markRunning();

                    return interviewAnswerRepository.save(answer);
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
                                    .findById(answerId)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.markDone();

                    interviewAnswerRepository.save(answer);
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
                                    .findById(answerId)
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "해당 답변을 찾을 수 없습니다. answerId="
                                                                    + answerId
                                                    )
                                    );

                    answer.markFailed(errorMessage);

                    interviewAnswerRepository.save(answer);
                }
        );
    }
}
