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
     * Controller 요청 진입점
     *
     * Client
     *  ↓
     * Spring
     *  ↓
     * FastAPI(STT + 분석 + Gemini)
     *  ↓
     * SpeechReport + AiEvaluation 저장
     */
    @Transactional
    public Long analyzeAndSave(
            SpeechAnalyzeRequest request
    ) {

        validateAnalyzeRequest(request);


        Map<String,Object> fastApiResponse =
                speechAnalysisService.analyzeSpeech(
                        request.getSourceType(),
                        request.getSourceValue(),
                        request.getJobCategory(),
                        request.getInterviewId(),
                        request.getInterviewAnswerId(),
                        request.getQuestionId(),
                        request.getOriginalQuestion()
                );


        Map<String,Object> payload =
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


        return saveReport(reportRequest);
    }




    /**
     * SpeechReport 저장
     *
     * 전체 영상 기반 분석 결과 저장
     * + 질문별 AI 평가 저장
     */
    @Transactional
    public Long saveReport(
            SpeechReportRequest request
    ) {

        validateReportRequest(request);


        try {

            InterviewEntity interview =
                    interviewRepository.findById(
                                    request.getInterviewId()
                            )
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "면접 정보를 찾을 수 없습니다."
                                    )
                            );



            InterviewAnswerEntity interviewAnswer =
                    findInterviewAnswerOrNull(
                            request.getInterviewAnswerId()
                    );



            SpeechReportEntity report =
                    SpeechReportEntity.builder()

                            .interview(interview)

                            .textScript(
                                    request.getTextScript()
                            )

                            .totalFillerCount(
                                    getTotalFillerCount(request)
                            )

                            .fillerDetailsJson(
                                    toJson(
                                            getFillerDetails(request)
                                    )
                            )

                            .repeatedExpressionsJson(
                                    toJson(
                                            getRepeatedExpressions(request)
                                    )
                            )

                            .paceEvaluation(
                                    getPaceEvaluation(request)
                            )

                            .paceFeedback(
                                    getPaceFeedback(request)
                            )

                            .charactersPerSecond(
                                    getCharactersPerSecond(request)
                            )


                            .totalSilenceSeconds(
                                    getTotalSilenceSeconds(request)
                            )

                            .pauseRatePercent(
                                    getPauseRatePercent(request)
                            )

                            .criticalSilenceCount(
                                    getCriticalSilenceCount(request)
                            )

                            .build();



            SpeechReportEntity saved =
                    speechReportRepository.save(report);



            saveAiEvaluation(
                    request,
                    interviewAnswer
            );



            log.info(
                    "SpeechReport 저장 완료 id={}, interviewId={}",
                    saved.getId(),
                    interview.getId()
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
     *
     * AiEvaluationEntity
     *      |
     *      1:1
     *      |
     * InterviewAnswerEntity
     */
    private void saveAiEvaluation(
            SpeechReportRequest request,
            InterviewAnswerEntity interviewAnswer
    ) throws JsonProcessingException {


        if (request.getAiEvaluation() == null) {
            return;
        }


        if (interviewAnswer == null) {

            log.warn(
                    "interviewAnswer가 없어 AI 평가 저장 생략"
            );

            return;
        }



        AiEvaluationEntity aiEvaluation =
                AiEvaluationEntity.builder()

                        .interviewAnswer(
                                interviewAnswer
                        )

                        .starEvaluationJson(
                                toJson(
                                        request.getAiEvaluation()
                                                .getStarScore()
                                )
                        )

                        .geminiFeedback(
                                request.getAiEvaluation()
                                        .getFeedback()
                        )

                        .keywordsJson(
                                toJson(
                                        request.getAiEvaluation()
                                                .getKeywords()
                                )
                        )

                        .modelAnswer(
                                request.getAiEvaluation()
                                        .getModelAnswer()
                        )

                        .build();



        aiEvaluationRepository.save(aiEvaluation);
    }




    @Transactional(readOnly = true)
    public SpeechReportEntity getReport(
            Long id
    ) {

        return speechReportRepository.findById(id)

                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "스피치 리포트를 찾을 수 없습니다."
                        )
                );
    }





    /**
     * FastAPI 응답 구조 처리
     *
     * {
     *    result:{
     *       text_script:"",
     *       ai_evaluation:{}
     *    }
     * }
     */
    @SuppressWarnings("unchecked")
    private Map<String,Object> extractSpeechPayload(
            Map<String,Object> response
    ) {


        Object result =
                response.get("result");


        if (result instanceof Map<?,?>) {

            return (Map<String,Object>) result;
        }


        return response;
    }





    /**
     * FastAPI 결과에 없는 값 보완
     *
     * Spring 요청값 우선
     */
    private SpeechReportRequest fillMissingMetadata(
            SpeechReportRequest reportRequest,
            SpeechAnalyzeRequest analyzeRequest
    ) {


        return reportRequest.toBuilder()

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
                        isBlank(reportRequest.getOriginalQuestion())
                                ? analyzeRequest.getOriginalQuestion()
                                : reportRequest.getOriginalQuestion()
                )


                .jobCategory(
                        isBlank(reportRequest.getJobCategory())
                                ? analyzeRequest.getJobCategory()
                                : reportRequest.getJobCategory()
                )


                .build();
    }





    private InterviewAnswerEntity findInterviewAnswerOrNull(
            Long id
    ) {

        if (id == null) {
            return null;
        }


        return interviewAnswerRepository.findById(id)

                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "답변 정보를 찾을 수 없습니다. id=" + id
                        )
                );
    }





    private Integer getTotalFillerCount(
            SpeechReportRequest request
    ) {

        if (request.getFillerWordAnalysis() == null ||
                request.getFillerWordAnalysis().getTotalCount() == null) {

            return 0;
        }


        return request.getFillerWordAnalysis()
                .getTotalCount();
    }





    private Map<String,Integer> getFillerDetails(
            SpeechReportRequest request
    ) {

        if (request.getFillerWordAnalysis() == null ||
                request.getFillerWordAnalysis().getDetails() == null) {

            return Collections.emptyMap();
        }


        return request.getFillerWordAnalysis()
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

        if (request.getSpeechPaceAnalysis() == null) {
            return null;
        }


        return request.getSpeechPaceAnalysis()
                .getEvaluation();
    }





    private String getPaceFeedback(
            SpeechReportRequest request
    ) {

        if (request.getSpeechPaceAnalysis() == null) {
            return null;
        }


        return request.getSpeechPaceAnalysis()
                .getFeedback();
    }





    private Double getCharactersPerSecond(
            SpeechReportRequest request
    ) {

        if (request.getSpeechPaceAnalysis() == null) {
            return null;
        }


        return request.getSpeechPaceAnalysis()
                .getCharactersPerSecond();
    }



    private Double getTotalSilenceSeconds(
            SpeechReportRequest request
    ) {

        if (request.getSilenceAnalysis() == null) {
            return null;
        }


        return request.getSilenceAnalysis()
                .getTotalSilenceSeconds();
    }





    private Double getPauseRatePercent(
            SpeechReportRequest request
    ) {

        if (request.getSilenceAnalysis() == null) {
            return null;
        }


        return request.getSilenceAnalysis()
                .getPauseRatePercent();
    }





    private Integer getCriticalSilenceCount(
            SpeechReportRequest request
    ) {

        if (request.getSilenceAnalysis() == null ||
                request.getSilenceAnalysis().getCriticalSilenceCount() == null) {

            return 0;
        }


        return request.getSilenceAnalysis()
                .getCriticalSilenceCount();
    }





    private String toJson(
            Object value
    ) throws JsonProcessingException {

        return objectMapper.writeValueAsString(value);
    }





    private void validateAnalyzeRequest(
            SpeechAnalyzeRequest request
    ) {


        if (request == null) {
            throw new IllegalArgumentException(
                    "분석 요청 값이 없습니다."
            );
        }


        if (isBlank(request.getSourceType())) {
            throw new IllegalArgumentException(
                    "sourceType 필요"
            );
        }


        if (isBlank(request.getSourceValue())) {
            throw new IllegalArgumentException(
                    "sourceValue 필요"
            );
        }


        if (request.getInterviewId() == null) {
            throw new IllegalArgumentException(
                    "interviewId 필요"
            );
        }
    }





    private void validateReportRequest(
            SpeechReportRequest request
    ) {


        if (request == null ||
                request.getInterviewId() == null) {

            throw new IllegalArgumentException(
                    "리포트 요청 정보 부족"
            );
        }
    }





    private boolean isBlank(
            String value
    ) {

        return value == null || value.isBlank();
    }

}