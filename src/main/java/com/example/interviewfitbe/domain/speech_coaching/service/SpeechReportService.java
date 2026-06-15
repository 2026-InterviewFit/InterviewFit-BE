package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.service.FollowUpService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechReportRequest;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechReportRepository speechReportRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final ObjectMapper objectMapper;
    private final FollowUpService followUpService;
    private final SpeechAnalysisService speechAnalysisService;

    /**
     * 클라이언트 요청 → FastAPI 분석 → DB 저장.
     */
    @Transactional
    public Long analyzeAndSave(SpeechAnalyzeRequest request) {
        validateAnalyzeRequest(request);

        Map<String, Object> fastApiResponse = speechAnalysisService.analyzeSpeech(
                request.getSourceType(),
                request.getSourceValue(),
                request.getJobCategory(),
                request.getInterviewId()
        );

        Map<String, Object> speechPayload = extractSpeechPayload(fastApiResponse);

        SpeechReportRequest reportRequest = objectMapper.convertValue(
                speechPayload,
                SpeechReportRequest.class
        );

        fillMissingMetadata(reportRequest, request);

        return saveReport(reportRequest);
    }

    /**
     * 질문별 답변 영상 async 분석용 메서드.
     *
     * InterviewAnswerAnalysisJobService에서 호출한다.
     */
    @Transactional
    public Long analyzeAndSave(InterviewAnswerEntity interviewAnswer) {
        if (interviewAnswer == null || interviewAnswer.getId() == null) {
            throw new IllegalArgumentException("interviewAnswer 정보가 필요합니다.");
        }

        InterviewAnswerEntity loadedAnswer = interviewAnswerRepository.findWithInterviewById(interviewAnswer.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 답변 영상을 찾을 수 없습니다. id=" + interviewAnswer.getId()
                ));

        InterviewEntity interview = loadedAnswer.getInterview();

        String jobCategory = "일반";

        if (interview.getJobRole() != null && interview.getJobRole().getName() != null) {
            jobCategory = interview.getJobRole().getName();
        }

        SpeechAnalyzeRequest request = new SpeechAnalyzeRequest();
        request.setSourceType("minio");
        request.setSourceValue(loadedAnswer.getVideoObjectKey());
        request.setJobCategory(jobCategory);
        request.setInterviewId(interview.getId());
        request.setInterviewAnswerId(loadedAnswer.getId());
        request.setQuestionId(loadedAnswer.getQuestionId());
        request.setOriginalQuestion(loadedAnswer.getQuestionText());

        return analyzeAndSave(request);
    }

    /**
     * 스피치 리포트 저장.
     *
     * 주의:
     * 현재 질문별 분석 저장 안정화를 위해 꼬리질문 자동 생성은 여기서 호출하지 않는다.
     * questionId가 questions 테이블에 없는 경우 FollowUpService가 예외를 던지고,
     * 같은 트랜잭션이 rollback-only가 되어 speech_report 저장까지 롤백될 수 있기 때문이다.
     */
    @Transactional
    public Long saveReport(SpeechReportRequest request) {
        validateReportRequest(request);

        try {
            InterviewEntity interview = interviewRepository.findById(request.getInterviewId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "해당 면접 정보를 찾을 수 없습니다. id=" + request.getInterviewId()
                    ));

            InterviewAnswerEntity interviewAnswer = findInterviewAnswerOrNull(request.getInterviewAnswerId());

            SpeechReportEntity report = SpeechReportEntity.builder()
                    .interview(interview)
                    .interviewAnswer(interviewAnswer)
                    .textScript(request.getTextScript())
                    .totalFillerCount(getTotalFillerCount(request))
                    .fillerDetailsJson(toJson(getFillerDetails(request)))
                    .paceEvaluation(getPaceEvaluation(request))
                    .paceFeedback(getPaceFeedback(request))
                    .charactersPerSecond(getCharactersPerSecond(request))
                    .totalSilenceSeconds(getTotalSilenceSeconds(request))
                    .criticalSilenceCount(getCriticalSilenceCount(request))
                    .aiEvaluationJson(toJson(getAiEvaluation(request)))
                    .modelAnswer(request.getModelAnswer())
                    .build();

            SpeechReportEntity saved = speechReportRepository.save(report);

            log.info(
                    "스피치 리포트 DB 저장 완료 - id: {}, interviewId: {}, interviewAnswerId: {}",
                    saved.getId(),
                    interview.getId(),
                    interviewAnswer == null ? null : interviewAnswer.getId()
            );

            return saved.getId();

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("리포트 데이터 변환 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public SpeechReportEntity getReport(Long id) {
        return speechReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "리포트를 찾을 수 없습니다. id=" + id
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSpeechPayload(Map<String, Object> fastApiResponse) {
        Object result = fastApiResponse.get("result");

        if (result instanceof Map<?, ?>) {
            return (Map<String, Object>) result;
        }

        return fastApiResponse;
    }

    private void fillMissingMetadata(
            SpeechReportRequest reportRequest,
            SpeechAnalyzeRequest analyzeRequest
    ) {
        if (reportRequest.getInterviewId() == null) {
            reportRequest.setInterviewId(analyzeRequest.getInterviewId());
        }

        if (reportRequest.getInterviewAnswerId() == null) {
            reportRequest.setInterviewAnswerId(analyzeRequest.getInterviewAnswerId());
        }

        if (reportRequest.getQuestionId() == null) {
            reportRequest.setQuestionId(analyzeRequest.getQuestionId());
        }

        if (isBlank(reportRequest.getOriginalQuestion())) {
            reportRequest.setOriginalQuestion(analyzeRequest.getOriginalQuestion());
        }

        if (isBlank(reportRequest.getJobCategory())) {
            reportRequest.setJobCategory(analyzeRequest.getJobCategory());
        }
    }

    private InterviewAnswerEntity findInterviewAnswerOrNull(Long interviewAnswerId) {
        if (interviewAnswerId == null) {
            return null;
        }

        return interviewAnswerRepository.findById(interviewAnswerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 답변 영상을 찾을 수 없습니다. id=" + interviewAnswerId
                ));
    }

    /**
     * 현재 saveReport()에서는 호출하지 않는다.
     *
     * 나중에 질문 테이블과 질문 생성 API 흐름이 완전히 연결되면
     * 별도 트랜잭션 또는 별도 async job으로 분리해서 다시 붙이는 것을 권장한다.
     */
    private void createFollowUpSafely(SpeechReportRequest request) {
        try {
            FollowUpCreateResponse followUpResponse = createFollowUpIfPossible(request);

            if (followUpResponse != null) {
                log.info(
                        "스피치 텍스트 기반 꼬리질문 생성 완료 - followUpId: {}, interviewId: {}",
                        followUpResponse.getFollowUpId(),
                        request.getInterviewId()
                );
            }

        } catch (Exception e) {
            log.warn(
                    "스피치 리포트 저장은 완료됐지만 꼬리질문 생성은 실패했습니다. interviewId={}, reason={}",
                    request.getInterviewId(),
                    e.getMessage()
            );
        }
    }

    private FollowUpCreateResponse createFollowUpIfPossible(SpeechReportRequest request) {
        if (isBlank(request.getTextScript())) {
            log.warn("textScript가 비어 있어 꼬리질문 생성을 건너뜁니다.");
            return null;
        }

        boolean hasQuestion =
                request.getQuestionId() != null ||
                        !isBlank(request.getOriginalQuestion());

        if (!hasQuestion) {
            log.warn("questionId/originalQuestion이 없어 꼬리질문 생성을 건너뜁니다.");
            return null;
        }

        FollowUpCreateRequest followUpRequest = new FollowUpCreateRequest();
        followUpRequest.setInterviewId(request.getInterviewId());
        followUpRequest.setQuestionId(request.getQuestionId());
        followUpRequest.setOriginalQuestion(request.getOriginalQuestion());
        followUpRequest.setJob(request.getJobCategory());
        followUpRequest.setAnswer(request.getTextScript());
        followUpRequest.setInterviewStyle("normal");

        return followUpService.createAndSave(followUpRequest);
    }

    private void validateAnalyzeRequest(SpeechAnalyzeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("스피치 분석 요청 값이 필요합니다.");
        }

        if (isBlank(request.getSourceType())) {
            throw new IllegalArgumentException("sourceType 값이 필요합니다.");
        }

        if (isBlank(request.getSourceValue())) {
            throw new IllegalArgumentException("sourceValue 값이 필요합니다.");
        }

        if (request.getInterviewId() == null) {
            throw new IllegalArgumentException("interviewId 값이 필요합니다.");
        }
    }

    private void validateReportRequest(SpeechReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("스피치 리포트 요청 값이 필요합니다.");
        }

        if (request.getInterviewId() == null) {
            throw new IllegalArgumentException("interviewId 값이 필요합니다.");
        }
    }

    private Integer getTotalFillerCount(SpeechReportRequest request) {
        if (request.getFillerWordAnalysis() == null ||
                request.getFillerWordAnalysis().getTotalCount() == null) {
            return 0;
        }

        return request.getFillerWordAnalysis().getTotalCount();
    }

    private Map<String, Integer> getFillerDetails(SpeechReportRequest request) {
        if (request.getFillerWordAnalysis() == null ||
                request.getFillerWordAnalysis().getDetails() == null) {
            return Collections.emptyMap();
        }

        return request.getFillerWordAnalysis().getDetails();
    }

    private String getPaceEvaluation(SpeechReportRequest request) {
        if (request.getSpeechPaceAnalysis() == null) {
            return null;
        }

        return request.getSpeechPaceAnalysis().getEvaluation();
    }

    private String getPaceFeedback(SpeechReportRequest request) {
        if (request.getSpeechPaceAnalysis() == null) {
            return null;
        }

        return request.getSpeechPaceAnalysis().getFeedback();
    }

    private Double getCharactersPerSecond(SpeechReportRequest request) {
        if (request.getSpeechPaceAnalysis() == null) {
            return null;
        }

        return request.getSpeechPaceAnalysis().getCharactersPerSecond();
    }

    private Double getTotalSilenceSeconds(SpeechReportRequest request) {
        if (request.getSilenceAnalysis() == null) {
            return null;
        }

        return request.getSilenceAnalysis().getTotalSilenceSeconds();
    }

    private Integer getCriticalSilenceCount(SpeechReportRequest request) {
        if (request.getSilenceAnalysis() == null) {
            return 0;
        }

        return request.getSilenceAnalysis().getCriticalSilenceCount();
    }

    private Map<String, Object> getAiEvaluation(SpeechReportRequest request) {
        if (request.getAiEvaluation() == null) {
            return Collections.emptyMap();
        }

        return request.getAiEvaluation();
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}