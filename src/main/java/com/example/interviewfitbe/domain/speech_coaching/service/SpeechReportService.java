package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechReportRequest;
import com.example.interviewfitbe.domain.speech_coaching.entity.AiEvaluationEntity;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechReportRepository speechReportRepository;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final ObjectMapper objectMapper;
    private final SpeechAnalysisService speechAnalysisService;


    /**
     * Speech 분석 + STT + Gemini + DB 저장
     *
     * 같은 interview/answer에 이미 speech_report와 ai_evaluation이 모두 존재하면
     * FastAPI를 다시 호출하지 않고 기존 결과를 재사용한다.
     */
    @Transactional
    public Long analyzeAndSave(
            SpeechAnalyzeRequest request
    ) {

        validateAnalyzeRequest(request);

        Optional<SpeechReportEntity> existingReport =
                speechReportRepository.findByInterview_Id(
                        request.getInterviewId()
                );

        boolean aiEvaluationAlreadyExists =
                request.getInterviewAnswerId() == null
                        || aiEvaluationRepository
                        .findByInterviewAnswer_Id(
                                request.getInterviewAnswerId()
                        )
                        .isPresent();

        /*
         * speech_report와 ai_evaluation이 이미 모두 존재하면
         * FastAPI를 다시 호출하지 않는다.
         */
        if (
                existingReport.isPresent()
                        && aiEvaluationAlreadyExists
        ) {

            log.info(
                    "[Speech] 기존 Speech/Gemini 분석 결과 재사용 - "
                            + "interviewId={}, answerId={}, speechReportId={}",
                    request.getInterviewId(),
                    request.getInterviewAnswerId(),
                    existingReport.get().getId()
            );

            return existingReport.get().getId();
        }

        /*
         * 1. FastAPI Speech 분석 호출
         * STT + 음성 분석 + Gemini 평가를 한 번의 요청으로 수행한다.
         */
        Map<String, Object> fastApiResponse =
                speechAnalysisService.analyzeSpeech(
                        request.getSourceType(),
                        request.getSourceValue(),
                        request.getJobCategory(),
                        request.getInterviewId(),
                        request.getInterviewAnswerId(),
                        request.getQuestionId(),
                        request.getOriginalQuestion()
                );

        log.info(
                "[Speech] FastAPI 전체 응답 = {}",
                fastApiResponse
        );

        /*
         * 2. 실제 Speech payload 추출
         */
        Map<String, Object> payload =
                extractSpeechPayload(
                        fastApiResponse
                );

        log.info(
                "[Speech] 추출된 payload = {}",
                payload
        );

        /*
         * 3. FastAPI 응답 -> DTO
         */
        SpeechReportRequest reportRequest =
                objectMapper.convertValue(
                        payload,
                        SpeechReportRequest.class
                );

        /*
         * 4. 메타데이터 보완
         */
        reportRequest =
                fillMissingMetadata(
                        reportRequest,
                        request
                );

        /*
         * 5. STT 보완
         *
         * DTO 변환 과정에서 textScript가 비어 있다면
         * FastAPI 전체 응답에서 직접 다시 검색한다.
         */
        if (
                reportRequest.getTextScript() == null
                        || reportRequest.getTextScript().isBlank()
        ) {

            String extractedText =
                    extractTextScript(
                            fastApiResponse
                    );

            if (
                    extractedText != null
                            && !extractedText.isBlank()
            ) {

                reportRequest =
                        reportRequest
                                .toBuilder()
                                .textScript(
                                        extractedText.trim()
                                )
                                .build();

                log.info(
                        "[Speech] STT fallback 추출 성공 - length={}",
                        extractedText.length()
                );
            }
        }

        /*
         * 6. 최종 STT 확인
         */
        if (
                reportRequest.getTextScript() == null
                        || reportRequest.getTextScript().isBlank()
        ) {

            log.warn(
                    "[Speech] STT 결과가 비어 있습니다. "
                            + "interviewId={}, answerId={}, sourceValue={}",
                    request.getInterviewId(),
                    request.getInterviewAnswerId(),
                    request.getSourceValue()
            );

        } else {

            log.info(
                    "[Speech] STT 정상 추출 - interviewId={}, answerId={}, length={}, text={}",
                    request.getInterviewId(),
                    request.getInterviewAnswerId(),
                    reportRequest.getTextScript().length(),
                    reportRequest.getTextScript()
            );
        }

        /*
         * 7. DB 저장
         */
        return saveReport(
                reportRequest
        );
    }

    /**
     * 일반 면접의 질문별 답변 분석.
     *
     * 질문별 audio/webm은 면접 전체 SpeechReport를 만들기 위한 원본이 아니다.
     * 따라서 speech_report(interview 단위)는 저장/갱신하지 않고,
     * 아래 두 결과만 답변 기준으로 저장한다.
     *
     * 1) interview_answers.answer_text : Whisper STT
     * 2) ai_evaluation                 : Gemini 질문별 내용 평가
     *
     * 전체 면접 영상의 스피치 지표/전체 STT는 기존 analyzeAndSave()가 담당한다.
     */
    @Transactional
    public void analyzeQuestionAnswerAndSave(
            SpeechAnalyzeRequest request
    ) {

        validateAnalyzeRequest(request);

        if (request.getInterviewAnswerId() == null) {
            throw new IllegalArgumentException(
                    "질문별 답변 분석에는 interviewAnswerId가 필요합니다."
            );
        }

        InterviewAnswerEntity interviewAnswer =
                findInterviewAnswerOrNull(
                        request.getInterviewAnswerId()
                );

        boolean hasAnswerText =
                interviewAnswer != null
                        && interviewAnswer.getAnswerText() != null
                        && !interviewAnswer.getAnswerText().isBlank();

        boolean hasAiEvaluation =
                aiEvaluationRepository
                        .findByInterviewAnswer_Id(
                                request.getInterviewAnswerId()
                        )
                        .isPresent();

        if (hasAnswerText && hasAiEvaluation) {
            log.info(
                    "[Speech] 기존 질문별 STT/Gemini 결과 재사용 - answerId={}",
                    request.getInterviewAnswerId()
            );
            return;
        }

        Map<String, Object> fastApiResponse =
                speechAnalysisService.analyzeSpeech(
                        request.getSourceType(),
                        request.getSourceValue(),
                        request.getJobCategory(),
                        request.getInterviewId(),
                        request.getInterviewAnswerId(),
                        request.getQuestionId(),
                        request.getOriginalQuestion()
                );

        log.info(
                "[Speech][Question] FastAPI 전체 응답 = {}",
                fastApiResponse
        );

        Map<String, Object> payload =
                extractSpeechPayload(
                        fastApiResponse
                );

        SpeechReportRequest reportRequest =
                objectMapper.convertValue(
                        payload,
                        SpeechReportRequest.class
                );

        reportRequest =
                fillMissingMetadata(
                        reportRequest,
                        request
                );

        if (
                reportRequest.getTextScript() == null
                        || reportRequest.getTextScript().isBlank()
        ) {
            String extractedText =
                    extractTextScript(
                            fastApiResponse
                    );

            if (extractedText != null && !extractedText.isBlank()) {
                reportRequest =
                        reportRequest
                                .toBuilder()
                                .textScript(extractedText.trim())
                                .build();
            }
        }

        String textScript = reportRequest.getTextScript();

        if (textScript != null && !textScript.isBlank()) {
            interviewAnswer.updateAnswerText(
                    textScript.trim()
            );
            interviewAnswerRepository.save(
                    interviewAnswer
            );

            log.info(
                    "[Speech][Question] STT 저장 완료 - answerId={}, textLength={}",
                    interviewAnswer.getId(),
                    textScript.trim().length()
            );
        } else {
            log.warn(
                    "[Speech][Question] STT 결과가 비어 있습니다. answerId={}",
                    interviewAnswer.getId()
            );
        }

        try {
            saveAiEvaluation(
                    reportRequest,
                    interviewAnswer
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "질문별 AI 평가 JSON 변환 실패",
                    e
            );
        }

        log.info(
                "[Speech][Question] 질문별 STT/Gemini 저장 완료 - answerId={}, questionId={}",
                interviewAnswer.getId(),
                request.getQuestionId()
        );
    }


    /**
     * Speech 점수 계산
     */
    private Double calculateSpeechScore(
            SpeechReportRequest request
    ) {

        double score = 100.0;

        int fillerCount =
                getTotalFillerCount(
                        request
                );

        if (fillerCount > 20) {

            score -= Math.min(
                    20.0,
                    fillerCount - 20.0
            );
        }

        Double pauseRate =
                getPauseRatePercent(
                        request
                );

        if (
                pauseRate != null
                        && pauseRate > 15.0
        ) {

            score -= Math.min(
                    20.0,
                    pauseRate - 15.0
            );
        }

        int criticalSilenceCount =
                getCriticalSilenceCount(
                        request
                );

        if (criticalSilenceCount > 3) {

            score -= Math.min(
                    20.0,
                    criticalSilenceCount * 2.0
            );
        }

        Double charactersPerSecond =
                getCharactersPerSecond(
                        request
                );

        if (
                charactersPerSecond != null
                        && (
                        charactersPerSecond < 3.0
                                || charactersPerSecond > 8.0
                )
        ) {

            score -= 10.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        100.0,
                        score
                )
        );
    }


    /**
     * SpeechReport + AiEvaluation 저장
     *
     * speech_report는 현재 interview 단위로 저장되므로,
     * 같은 interview의 리포트가 이미 있으면 중복 INSERT하지 않고 기존 행을 재사용한다.
     *
     * ai_evaluation은 interview_answer_id 기준으로 중복 저장을 방지한다.
     */
    @Transactional
    public Long saveReport(
            SpeechReportRequest request
    ) {

        validateReportRequest(
                request
        );

        try {

            InterviewEntity interview =
                    interviewRepository
                            .findById(
                                    request.getInterviewId()
                            )
                            .orElseThrow(
                                    () ->
                                            new EntityNotFoundException(
                                                    "면접 정보를 찾을 수 없습니다."
                                            )
                            );

            InterviewAnswerEntity interviewAnswer =
                    findInterviewAnswerOrNull(
                            request.getInterviewAnswerId()
                    );

            String textScript =
                    request.getTextScript();

            if (textScript != null) {
                textScript = textScript.trim();
            }

            Optional<SpeechReportEntity> existingReport =
                    speechReportRepository.findByInterview_Id(
                            request.getInterviewId()
                    );

            SpeechReportEntity saved;

            if (existingReport.isPresent()) {

                saved = existingReport.get();

                log.info(
                        "[Speech] 기존 SpeechReport 재사용 - reportId={}, interviewId={}",
                        saved.getId(),
                        interview.getId()
                );

            } else {

                SpeechReportEntity report =
                        SpeechReportEntity
                                .builder()
                                .interview(
                                        interview
                                )
                                .score(
                                        calculateSpeechScore(
                                                request
                                        )
                                )
                                .textScript(
                                        textScript
                                )
                                .totalFillerCount(
                                        getTotalFillerCount(
                                                request
                                        )
                                )
                                .fillerDetailsJson(
                                        toJson(
                                                getFillerDetails(
                                                        request
                                                )
                                        )
                                )
                                .repeatedExpressionsJson(
                                        toJson(
                                                getRepeatedExpressions(
                                                        request
                                                )
                                        )
                                )
                                .paceEvaluation(
                                        getPaceEvaluation(
                                                request
                                        )
                                )
                                .paceFeedback(
                                        getPaceFeedback(
                                                request
                                        )
                                )
                                .charactersPerSecond(
                                        getCharactersPerSecond(
                                                request
                                        )
                                )
                                .totalSilenceSeconds(
                                        getTotalSilenceSeconds(
                                                request
                                        )
                                )
                                .pauseRatePercent(
                                        getPauseRatePercent(
                                                request
                                        )
                                )
                                .criticalSilenceCount(
                                        getCriticalSilenceCount(
                                                request
                                        )
                                )
                                .build();

                saved =
                        speechReportRepository.save(
                                report
                        );
            }

            saveAiEvaluation(
                    request,
                    interviewAnswer
            );

            log.info(
                    "[Speech] SpeechReport 저장/확인 완료 - "
                            + "reportId={}, interviewId={}, answerId={}, textLength={}",
                    saved.getId(),
                    interview.getId(),
                    request.getInterviewAnswerId(),
                    textScript == null
                            ? 0
                            : textScript.length()
            );

            return saved.getId();

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "JSON 변환 실패",
                    e
            );
        }
    }


    /**
     * 질문별 AI 평가 저장
     */
    private void saveAiEvaluation(
            SpeechReportRequest request,
            InterviewAnswerEntity interviewAnswer
    ) throws JsonProcessingException {

        if (
                request.getAiEvaluation() == null
        ) {

            log.warn(
                    "[Speech] FastAPI 응답에 ai_evaluation이 없어 AI 평가 저장 생략 - "
                            + "interviewId={}, answerId={}",
                    request.getInterviewId(),
                    request.getInterviewAnswerId()
            );

            return;
        }

        /*
         * PT 발표 전체 영상처럼 interviewAnswerId가 없는 경우에는
         * 질문별 AI 평가를 저장하지 않는다.
         */
        if (
                interviewAnswer == null
        ) {

            log.info(
                    "[Speech] interviewAnswer 없음 - "
                            + "전체 발표 Speech 분석으로 판단, AI 평가 저장 생략"
            );

            return;
        }

        if (
                aiEvaluationRepository
                        .findByInterviewAnswer_Id(
                                interviewAnswer.getId()
                        )
                        .isPresent()
        ) {

            log.info(
                    "[Speech] 기존 AiEvaluation 재사용 - answerId={}",
                    interviewAnswer.getId()
            );

            return;
        }

        AiEvaluationEntity aiEvaluation =
                AiEvaluationEntity
                        .builder()
                        .interviewAnswer(
                                interviewAnswer
                        )
                        .starEvaluationJson(
                                toJson(
                                        request
                                                .getAiEvaluation()
                                                .getStarScore()
                                )
                        )
                        .geminiFeedback(
                                request
                                        .getAiEvaluation()
                                        .getFeedback()
                        )
                        .keywordsJson(
                                toJson(
                                        request
                                                .getAiEvaluation()
                                                .getKeywords()
                                )
                        )
                        .modelAnswer(
                                request
                                        .getAiEvaluation()
                                        .getModelAnswer()
                        )
                        .build();

        aiEvaluationRepository.save(
                aiEvaluation
        );

        log.info(
                "[Speech] AiEvaluation 저장 완료 - answerId={}",
                interviewAnswer.getId()
        );
    }

    /**
     * SpeechReport 조회
     */
    @Transactional(readOnly = true)
    public SpeechReportEntity getReport(
            Long id
    ) {

        return speechReportRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "스피치 리포트를 찾을 수 없습니다."
                        )
                );
    }

    /**
     * FastAPI 응답 payload 추출
     *
     * 대응:
     *
     * {
     *   "result": {...}
     * }
     *
     * {
     *   "data": {...}
     * }
     *
     * {
     *   "payload": {...}
     * }
     *
     * 또는 최상위 직접 응답
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSpeechPayload(
            Map<String, Object> response
    ) {

        if (
                response == null
                        || response.isEmpty()
        ) {

            return Collections.emptyMap();
        }

        Object result =
                response.get(
                        "result"
                );

        if (
                result instanceof Map<?, ?>
        ) {

            return (Map<String, Object>) result;
        }

        Object data =
                response.get(
                        "data"
                );

        if (
                data instanceof Map<?, ?>
        ) {

            return (Map<String, Object>) data;
        }

        Object payload =
                response.get(
                        "payload"
                );

        if (
                payload instanceof Map<?, ?>
        ) {

            return (Map<String, Object>) payload;
        }

        return response;
    }

    /**
     * FastAPI 전체 응답에서 STT 텍스트를 찾는다.
     */
    private String extractTextScript(
            Map<String, Object> response
    ) {

        if (
                response == null
                        || response.isEmpty()
        ) {
            return null;
        }

        /*
         * 현재 Map에서 바로 검색
         */
        String value =
                getStringValue(
                        response,
                        "text_script"
                );

        if (
                value != null
                        && !value.isBlank()
        ) {
            return value;
        }

        value =
                getStringValue(
                        response,
                        "textScript"
                );

        if (
                value != null
                        && !value.isBlank()
        ) {
            return value;
        }

        value =
                getStringValue(
                        response,
                        "transcript"
                );

        if (
                value != null
                        && !value.isBlank()
        ) {
            return value;
        }

        /*
         * 내부 Map 재귀 검색
         */
        for (
                Object child :
                response.values()
        ) {

            if (
                    child instanceof Map<?, ?>
            ) {

                @SuppressWarnings("unchecked")
                Map<String, Object> childMap =
                        (Map<String, Object>) child;

                String nested =
                        extractTextScript(
                                childMap
                        );

                if (
                        nested != null
                                && !nested.isBlank()
                ) {

                    return nested;
                }
            }
        }

        return null;
    }

    private String getStringValue(
            Map<String, Object> map,
            String key
    ) {

        Object value =
                map.get(
                        key
                );

        if (
                value == null
        ) {
            return null;
        }

        return String
                .valueOf(
                        value
                )
                .trim();
    }

    /**
     * FastAPI 결과에서 빠진 메타데이터 보완
     */
    private SpeechReportRequest fillMissingMetadata(
            SpeechReportRequest reportRequest,
            SpeechAnalyzeRequest analyzeRequest
    ) {

        return reportRequest
                .toBuilder()

                .interviewId(
                        reportRequest.getInterviewId() != null
                                ? reportRequest.getInterviewId()
                                : analyzeRequest.getInterviewId()
                )

                .interviewAnswerId(
                        reportRequest.getInterviewAnswerId() != null
                                ? reportRequest.getInterviewAnswerId()
                                : analyzeRequest.getInterviewAnswerId()
                )

                .questionId(
                        reportRequest.getQuestionId() != null
                                ? reportRequest.getQuestionId()
                                : analyzeRequest.getQuestionId()
                )

                .originalQuestion(
                        isBlank(
                                reportRequest.getOriginalQuestion()
                        )
                                ? analyzeRequest.getOriginalQuestion()
                                : reportRequest.getOriginalQuestion()
                )

                .jobCategory(
                        isBlank(
                                reportRequest.getJobCategory()
                        )
                                ? analyzeRequest.getJobCategory()
                                : reportRequest.getJobCategory()
                )

                .build();
    }

    private InterviewAnswerEntity findInterviewAnswerOrNull(
            Long id
    ) {

        if (
                id == null
        ) {
            return null;
        }

        return interviewAnswerRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "답변 정보를 찾을 수 없습니다. id="
                                        + id
                        )
                );
    }

    private Integer getTotalFillerCount(
            SpeechReportRequest request
    ) {

        if (
                request.getFillerWordAnalysis() == null
                        || request
                        .getFillerWordAnalysis()
                        .getTotalCount() == null
        ) {

            return 0;
        }

        return request
                .getFillerWordAnalysis()
                .getTotalCount();
    }

    private Map<String, Integer> getFillerDetails(
            SpeechReportRequest request
    ) {

        if (
                request.getFillerWordAnalysis() == null
                        || request
                        .getFillerWordAnalysis()
                        .getDetails() == null
        ) {

            return Collections.emptyMap();
        }

        return request
                .getFillerWordAnalysis()
                .getDetails();
    }

    private List<SpeechReportRequest.RepeatedExpression>
    getRepeatedExpressions(
            SpeechReportRequest request
    ) {

        return request.getRepeatedExpressions() == null
                ? Collections.emptyList()
                : request.getRepeatedExpressions();
    }

    private String getPaceEvaluation(
            SpeechReportRequest request
    ) {

        if (
                request.getSpeechPaceAnalysis() == null
        ) {
            return null;
        }

        return request
                .getSpeechPaceAnalysis()
                .getEvaluation();
    }

    private String getPaceFeedback(
            SpeechReportRequest request
    ) {

        if (
                request.getSpeechPaceAnalysis() == null
        ) {
            return null;
        }

        return request
                .getSpeechPaceAnalysis()
                .getFeedback();
    }

    private Double getCharactersPerSecond(
            SpeechReportRequest request
    ) {

        if (
                request.getSpeechPaceAnalysis() == null
        ) {
            return null;
        }

        return request
                .getSpeechPaceAnalysis()
                .getCharactersPerSecond();
    }

    private Double getTotalSilenceSeconds(
            SpeechReportRequest request
    ) {

        if (
                request.getSilenceAnalysis() == null
        ) {
            return null;
        }

        return request
                .getSilenceAnalysis()
                .getTotalSilenceSeconds();
    }

    private Double getPauseRatePercent(
            SpeechReportRequest request
    ) {

        if (
                request.getSilenceAnalysis() == null
        ) {
            return null;
        }

        return request
                .getSilenceAnalysis()
                .getPauseRatePercent();
    }

    private Integer getCriticalSilenceCount(
            SpeechReportRequest request
    ) {

        if (
                request.getSilenceAnalysis() == null
                        || request
                        .getSilenceAnalysis()
                        .getCriticalSilenceCount() == null
        ) {

            return 0;
        }

        return request
                .getSilenceAnalysis()
                .getCriticalSilenceCount();
    }

    private String toJson(
            Object value
    ) throws JsonProcessingException {

        return objectMapper
                .writeValueAsString(
                        value
                );
    }

    private void validateAnalyzeRequest(
            SpeechAnalyzeRequest request
    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "분석 요청 값이 없습니다."
            );
        }

        if (
                isBlank(
                        request.getSourceType()
                )
        ) {

            throw new IllegalArgumentException(
                    "sourceType 필요"
            );
        }

        if (
                isBlank(
                        request.getSourceValue()
                )
        ) {

            throw new IllegalArgumentException(
                    "sourceValue 필요"
            );
        }

        if (
                request.getInterviewId() == null
        ) {

            throw new IllegalArgumentException(
                    "interviewId 필요"
            );
        }
    }

    private void validateReportRequest(
            SpeechReportRequest request
    ) {

        if (
                request == null
                        || request.getInterviewId() == null
        ) {

            throw new IllegalArgumentException(
                    "리포트 요청 정보 부족"
            );
        }
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }
}
