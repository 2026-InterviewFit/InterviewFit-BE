package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.eye_gaze.service.EyeGazeService;
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
    private final EyeGazeService eyeGazeService;
    private final PoseService poseService;
    private final SpeechReportService speechReportService;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 답변 영상 분석
     *
     * - eye blinking
     * - eye gaze
     * - pose
     * - speech / STT / Gemini
     *
     * 일반 면접:
     * SpeechReportService.analyzeAndSave()
     *
     * PT 질의응답:
     * SpeechReportService.analyzeQuestionAnswerAndSave()
     *
     * PT 질의응답의 경우 발표 전체 SpeechReport를 사용하지 않고
     * 각 InterviewAnswer.answerText에 질문별 Whisper STT를 저장한다.
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
            log.info(
                    "눈 깜빡임 분석 시작: answerId={}",
                    answerId
            );

            eyeBlinkingService.analyzeAndSave(
                    videoObjectKey,
                    interview
            );

            log.info(
                    "눈 깜빡임 분석 완료: answerId={}",
                    answerId
            );

            /*
             * 2. 시선 분석
             */
            log.info(
                    "시선 분석 시작: answerId={}",
                    answerId
            );

            eyeGazeService.analyzeAndSave(
                    videoObjectKey,
                    interview
            );

            log.info(
                    "시선 분석 완료: answerId={}",
                    answerId
            );

            /*
             * 3. 자세 분석
             */
            log.info(
                    "자세 분석 시작: answerId={}",
                    answerId
            );

            poseService.processPostureAnalysis(
                    videoObjectKey,
                    interview
            );

            log.info(
                    "자세 분석 완료: answerId={}",
                    answerId
            );

            /*
             * 4. Speech / STT / Gemini 분석 + DB 저장
             */
            analyzeSpeechGeminiAndSave(
                    answerId,
                    videoObjectKey,
                    interview,
                    answer
            );

            /*
             * Gemini 평가 결과가 실제 DB에 저장됐는지 검증
             */
            if (
                    aiEvaluationRepository
                            .findByInterviewAnswer_Id(answerId)
                            .isEmpty()
            ) {

                throw new IllegalStateException(
                        "Gemini AI 평가 결과가 저장되지 않았습니다. answerId="
                                + answerId
                );
            }

            /*
             * 5. 분석 완료 처리
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

    /**
     * Speech / STT / Gemini 분석 및 저장
     *
     * 일반 면접:
     *
     * InterviewAnswerAnalysisJobService
     *      -> SpeechReportService.analyzeAndSave()
     *      -> SpeechAnalysisService.analyzeSpeech()
     *      -> FastAPI /apis/speech/analyze
     *      -> speech_report 저장
     *      -> ai_evaluation 저장
     *      -> SpeechReport STT를 answerText에 저장
     *
     *
     * PT 질의응답:
     *
     * InterviewAnswerAnalysisJobService
     *      -> SpeechReportService.analyzeQuestionAnswerAndSave()
     *      -> SpeechAnalysisService.analyzeSpeech()
     *      -> FastAPI /apis/speech/analyze
     *      -> InterviewAnswer.answerText에 질문별 STT 저장
     *      -> ai_evaluation 저장
     *
     * PT 질문 답변은 전체 발표 SpeechReport를 갱신하지 않는다.
     */
    private void analyzeSpeechGeminiAndSave(
            Long answerId,
            String videoObjectKey,
            InterviewEntity interview,
            InterviewAnswerEntity answer
    ) {

        /*
         * 직무 카테고리
         */
        String jobCategory = "";

        if (
                interview.getJobRole() != null
                        && interview.getJobRole().getCategory() != null
        ) {

            jobCategory =
                    interview
                            .getJobRole()
                            .getCategory();
        }

        /*
         * 질문 ID
         */
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

        /*
         * 질문 내용
         */
        String originalQuestion =
                answer.getQuestionText();

        /*
         * FastAPI Speech 분석 요청
         */
        SpeechAnalyzeRequest request =
                SpeechAnalyzeRequest
                        .builder()
                        .sourceType("minio")
                        .sourceValue(videoObjectKey)
                        .jobCategory(jobCategory)
                        .interviewId(interview.getId())
                        .interviewAnswerId(answerId)
                        .questionId(questionId)
                        .originalQuestion(originalQuestion)
                        .build();

        /*
         * =========================================================
         * PT 질의응답
         * =========================================================
         *
         * PT 질문별 답변은 발표 전체 SpeechReport를 사용하면 안 된다.
         *
         * 질문마다 녹화된 pt-answer-XXX.webm을 Whisper로 분석한 뒤
         * 해당 InterviewAnswer.answerText에 직접 저장한다.
         */
        if (answer.getPtQuestion() != null) {

            log.info(
                    "[PT][Question] Speech/Gemini 분석 시작: answerId={}, interviewId={}, questionId={}",
                    answerId,
                    interview.getId(),
                    questionId
            );

            speechReportService
                    .analyzeQuestionAnswerAndSave(
                            request
                    );

            log.info(
                    "[PT][Question] Speech/Gemini 분석 완료: answerId={}, questionId={}",
                    answerId,
                    questionId
            );

            return;
        }

        /*
         * =========================================================
         * 일반 면접 답변
         * =========================================================
         */
        log.info(
                "Speech/Gemini 분석 시작: answerId={}, interviewId={}",
                answerId,
                interview.getId()
        );

        /*
         * 일반 면접은 기존 SpeechReport 방식 유지
         */
        Long speechReportId =
                speechReportService
                        .analyzeAndSave(
                                request
                        );

        SpeechReportEntity speechReport =
                speechReportService
                        .getReport(
                                speechReportId
                        );

        String textScript =
                speechReport.getTextScript();

        /*
         * 일반 면접 STT를 InterviewAnswer에도 저장
         */
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

    /**
     * 일반 면접 STT 결과 저장
     *
     * PT 질의응답에서는 이 메서드를 사용하지 않는다.
     * PT는 SpeechReportService.analyzeQuestionAnswerAndSave()
     * 내부에서 직접 answerText를 저장한다.
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

                    answer.updateAnswerText(
                            answerText
                    );

                    interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }

    /**
     * 분석 시작 상태
     */
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

                    return interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }

    /**
     * 분석 완료 상태
     */
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

                    interviewAnswerRepository.save(
                            answer
                    );
                }
        );
    }

    /**
     * 분석 실패 상태
     */
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
