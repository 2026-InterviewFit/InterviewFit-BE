package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 일반 면접의 "질문별 답변" 비동기 분석 Job.
 *
 * 질문별 저장 파일은 현재 audio/webm 이므로
 * 눈 깜빡임 / 자세 같은 영상 분석은 수행하지 않는다.
 *
 * 처리 범위:
 * 1. STT
 * 2. Gemini 질문별 내용 평가
 * 3. interview_answers.answer_text 저장
 * 4. ai_evaluation 저장
 * 5. interview_answers.analysis_status DONE/FAILED 갱신
 *
 * 전체 면접 video/webm 분석은 InterviewAnswerAnalysisJobService가 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAnswerAnalysisJobService {

    private final InterviewAnswerRepository interviewAnswerRepository;
    private final SpeechReportService speechReportService;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final TransactionTemplate transactionTemplate;

    @Async("analysisExecutor")
    public void analyzeQuestionAnswerAsync(
            Long answerId,
            String sourceObjectKey
    ) {

        log.info(
                "질문별 답변 분석 시작: answerId={}, sourceObjectKey={}",
                answerId,
                sourceObjectKey
        );

        InterviewAnswerEntity answer = markRunning(answerId);

        try {

            InterviewEntity interview = answer.getInterviewSafely();

            if (interview == null) {
                throw new IllegalStateException(
                        "질문별 답변에 연결된 면접 정보를 찾을 수 없습니다. answerId="
                                + answerId
                );
            }

            Long questionId = answer.getQuestionId();
            String originalQuestion = answer.getQuestionText();

            if (questionId == null || originalQuestion == null || originalQuestion.isBlank()) {
                throw new IllegalStateException(
                        "질문별 답변에 연결된 질문 정보를 찾을 수 없습니다. answerId="
                                + answerId
                );
            }

            String jobCategory = "";

            if (
                    interview.getJobRole() != null
                            && interview.getJobRole().getCategory() != null
            ) {
                jobCategory = interview.getJobRole().getCategory();
            }

            SpeechAnalyzeRequest request =
                    SpeechAnalyzeRequest.builder()
                            .sourceType("minio")
                            .sourceValue(sourceObjectKey)
                            .jobCategory(jobCategory)
                            .interviewId(interview.getId())
                            .interviewAnswerId(answerId)
                            .questionId(questionId)
                            .originalQuestion(originalQuestion)
                            .build();

            /*
             * 질문별 분석에서는 speech_report(면접 전체용)를 건드리지 않는다.
             * STT + Gemini 결과만 InterviewAnswer / AiEvaluation에 저장한다.
             */
            speechReportService.analyzeQuestionAnswerAndSave(request);

            if (
                    aiEvaluationRepository
                            .findByInterviewAnswer_Id(answerId)
                            .isEmpty()
            ) {
                throw new IllegalStateException(
                        "질문별 Gemini 평가 결과가 저장되지 않았습니다. answerId="
                                + answerId
                );
            }

            markDone(answerId);

            log.info(
                    "질문별 답변 분석 완료: answerId={}, questionId={}",
                    answerId,
                    questionId
            );

        } catch (Exception e) {

            log.error(
                    "질문별 답변 분석 실패: answerId={}, error={}",
                    answerId,
                    e.getMessage(),
                    e
            );

            markFailed(answerId, e.getMessage());
        }
    }

    private InterviewAnswerEntity markRunning(Long answerId) {

        return transactionTemplate.execute(
                status -> {
                    InterviewAnswerEntity answer =
                            interviewAnswerRepository
                                    .findWithInterviewById(answerId)
                                    .orElseThrow(
                                            () -> new IllegalArgumentException(
                                                    "해당 답변을 찾을 수 없습니다. answerId="
                                                            + answerId
                                            )
                                    );

                    answer.markRunning();
                    return interviewAnswerRepository.save(answer);
                }
        );
    }

    private void markDone(Long answerId) {

        transactionTemplate.executeWithoutResult(
                status -> {
                    InterviewAnswerEntity answer =
                            interviewAnswerRepository
                                    .findById(answerId)
                                    .orElseThrow(
                                            () -> new IllegalArgumentException(
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
                                            () -> new IllegalArgumentException(
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
