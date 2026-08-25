package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
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

    /**
     * 전체 영상 음성 분석 + Whisper STT + Gemini 내용 평가를 담당한다.
     *
     * SpeechReportService 내부 흐름:
     *
     * Spring
     *  -> FastAPI /apis/speech/analyze
     *  -> Whisper STT
     *  -> 습관어 / 속도 / 침묵 분석
     *  -> Gemini STAR 내용 평가
     *  -> speech_report 저장
     *  -> ai_evaluation 저장
     */
    private final SpeechReportService speechReportService;

    private final TransactionTemplate transactionTemplate;


    /**
     * 전체 면접 영상 비동기 분석.
     *
     * 현재 연결 기능:
     * - eye blinking
     * - pose
     * - speech
     * - Gemini 내용 평가
     *
     * 아직 별도 연결이 필요한 기능:
     * - eye gaze
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
             * =====================================================
             * 1. 눈 깜빡임 분석
             * =====================================================
             *
             * Spring -> FastAPI
             * POST /apis/eye-blinking/analyze
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
             * =====================================================
             * 2. 자세 분석
             * =====================================================
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
             * =====================================================
             * 3. Speech + Gemini 내용 분석
             * =====================================================
             *
             * 현재 프론트는 면접 전체 영상을 하나의 interview_answer로
             * 업로드하므로 해당 answerId를 AI 평가와 연결한다.
             *
             * sourceType = minio
             * sourceValue = MinIO object key
             */
            log.info(
                    "Speech/Gemini 분석 시작: answerId={}, interviewId={}",
                    answerId,
                    interview.getId()
            );


            String jobCategory = "일반";

            if (interview.getJobRole() != null) {

                /*
                 * FastAPI의 ai_evaluator_service에서는
                 * job_category 값을 실제 '직무명'처럼 사용한다.
                 *
                 * 따라서 "IT/개발" 같은 대분류보다
                 * "백엔드", "프론트엔드" 같은 직무명을 우선 전달한다.
                 */
                if (
                        interview.getJobRole().getName() != null
                                && !interview.getJobRole().getName().isBlank()
                ) {

                    jobCategory =
                            interview.getJobRole().getName();

                } else if (
                        interview.getJobRole().getCategory() != null
                                && !interview.getJobRole().getCategory().isBlank()
                ) {

                    jobCategory =
                            interview.getJobRole().getCategory();
                }
            }


            SpeechAnalyzeRequest speechAnalyzeRequest =
                    SpeechAnalyzeRequest.builder()

                            .sourceType(
                                    "minio"
                            )

                            .sourceValue(
                                    videoObjectKey
                            )

                            .jobCategory(
                                    jobCategory
                            )

                            .interviewId(
                                    interview.getId()
                            )

                            .interviewAnswerId(
                                    answerId
                            )

                            .questionId(
                                    answer.getQuestionId()
                            )

                            .originalQuestion(
                                    answer.getQuestionText()
                            )

                            .build();


            Long speechReportId =
                    speechReportService.analyzeAndSave(
                            speechAnalyzeRequest
                    );


            log.info(
                    "Speech/Gemini 분석 완료: answerId={}, speechReportId={}",
                    answerId,
                    speechReportId
            );


            /*
             * 모든 현재 연결 분석이 정상 완료된 경우에만 DONE 처리.
             */
            markDone(
                    answerId
            );


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
     * 분석 상태를 RUNNING으로 변경하면서
     * interview / jobRole까지 함께 조회한다.
     */
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
                                            () -> new IllegalArgumentException(
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
                                            () -> new IllegalArgumentException(
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
                                            () -> new IllegalArgumentException(
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