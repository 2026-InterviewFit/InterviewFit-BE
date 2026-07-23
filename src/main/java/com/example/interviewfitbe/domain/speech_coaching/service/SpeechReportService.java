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
import java.util.List;
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

        reportRequest = fillMissingMetadata(reportRequest, request);

        return saveReport(reportRequest);
    }

    /**
     * 질문별 답변 영상 async 분석용 메서드.
     *
     * InterviewAnswerAnalysisJobService에서 호출한다.
     */
//    @Transactional
//    public Long analyzeAndSave(InterviewAnswerEntity interviewAnswer) {
//        if (interviewAnswer == null || interviewAnswer.getId() == null) {
//            throw new IllegalArgumentException("interviewAnswer 정보가 필요합니다.");
//        }
//
//        InterviewAnswerEntity loadedAnswer = interviewAnswerRepository.findWithInterviewById(interviewAnswer.getId())
//                .orElseThrow(() -> new EntityNotFoundException(
//                        "해당 답변 영상을 찾을 수 없습니다. id=" + interviewAnswer.getId()
//                ));
//
//        InterviewEntity interview = loadedAnswer.getInterview();
//
//        String jobCategory = "일반";
//
//        if (interview.getJobRole() != null && interview.getJobRole().getName() != null) {
//            jobCategory = interview.getJobRole().getName();
//        }
//
//        SpeechAnalyzeRequest request = SpeechAnalyzeRequest.builder()
//                .sourceType("minio")
//                .sourceValue(loadedAnswer.getVideoObjectKey())
//                .jobCategory(jobCategory)
//                .interviewId(interview.getId())
//                .interviewAnswerId(loadedAnswer.getId())
//                .questionId(loadedAnswer.getQuestionId())
//                .originalQuestion(loadedAnswer.getQuestionText())
//                .build();
//
//        return analyzeAndSave(request);
//    }

    @Transactional
    public Long analyzeAndSave(InterviewAnswerEntity interviewAnswer) {
        if (interviewAnswer == null || interviewAnswer.getId() == null) {
            throw new IllegalArgumentException("interviewAnswer 정보가 필요합니다.");
        }

        InterviewAnswerEntity loadedAnswer = interviewAnswerRepository.findWithInterviewById(interviewAnswer.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 답변 정보를 찾을 수 없습니다. id=" + interviewAnswer.getId()
                ));

        InterviewEntity interview = loadedAnswer.getInterviewSafely();

        if (interview == null) {
            throw new IllegalStateException("답변에 연결된 면접 정보를 찾을 수 없습니다. answerId=" + loadedAnswer.getId());
        }

        String jobCategory = "일반";

        if (interview.getJobRole() != null && interview.getJobRole().getName() != null) {
            jobCategory = interview.getJobRole().getName();
        }

        /*
         * 현재 전체 영상 기반 구조로 바뀌었으므로
         * answer.videoObjectKey는 사용하지 않는다.
         *
         * 실제 분석 연결은 videos 테이블에서 interview_id로 objectKey를 조회하는 흐름이 붙은 뒤 처리.
         */
        throw new UnsupportedOperationException(
                "현재 speech 질문별 분석은 전체 영상 기반 구조로 전환 중입니다. videos 테이블 연동 후 다시 연결해야 합니다."
        );
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
                    .repeatedExpressionsJson(toJson(getRepeatedExpressions(request)))
                    .paceEvaluation(getPaceEvaluation(request))
                    .paceFeedback(getPaceFeedback(request))
                    .charactersPerSecond(getCharactersPerSecond(request))
                    .volumeDbMean(getVolumeDbMean(request))
                    .volumeDbStd(getVolumeDbStd(request))
                    .totalSilenceSeconds(getTotalSilenceSeconds(request))
                    .pauseRatePercent(getPauseRatePercent(request))
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

    /**
     * reportRequest에 빠진 메타데이터(클라이언트 요청 값)를 채운 새 객체를 반환한다.
     *
     * SpeechReportRequest가 불변(builder) 객체로 바뀌었으므로,
     * setter로 직접 수정하는 대신 toBuilder()로 복사한 새 인스턴스를 만든다.
     */
    private SpeechReportRequest fillMissingMetadata(
            SpeechReportRequest reportRequest,
            SpeechAnalyzeRequest analyzeRequest
    ) {
        return reportRequest.toBuilder()
                .interviewId(reportRequest.getInterviewId() != null
                        ? reportRequest.getInterviewId()
                        : analyzeRequest.getInterviewId())
                .interviewAnswerId(reportRequest.getInterviewAnswerId() != null
                        ? reportRequest.getInterviewAnswerId()
                        : analyzeRequest.getInterviewAnswerId())
                .questionId(reportRequest.getQuestionId() != null
                        ? reportRequest.getQuestionId()
                        : analyzeRequest.getQuestionId())
                .originalQuestion(isBlank(reportRequest.getOriginalQuestion())
                        ? analyzeRequest.getOriginalQuestion()
                        : reportRequest.getOriginalQuestion())
                .jobCategory(isBlank(reportRequest.getJobCategory())
                        ? analyzeRequest.getJobCategory()
                        : reportRequest.getJobCategory())
                .build();
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

    private List<SpeechReportRequest.RepeatedExpression> getRepeatedExpressions(SpeechReportRequest request) {
        if (request.getRepeatedExpressions() == null) {
            return Collections.emptyList();
        }

        return request.getRepeatedExpressions();
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

    private Double getVolumeDbMean(SpeechReportRequest request) {
        if (request.getVolumeAnalysis() == null) {
            return null;
        }

        return request.getVolumeAnalysis().getVolumeDbMean();
    }

    private Double getVolumeDbStd(SpeechReportRequest request) {
        if (request.getVolumeAnalysis() == null) {
            return null;
        }

        return request.getVolumeAnalysis().getVolumeDbStd();
    }

    private Double getTotalSilenceSeconds(SpeechReportRequest request) {
        if (request.getSilenceAnalysis() == null) {
            return null;
        }

        return request.getSilenceAnalysis().getTotalSilenceSeconds();
    }

    private Double getPauseRatePercent(SpeechReportRequest request) {
        if (request.getSilenceAnalysis() == null) {
            return null;
        }

        return request.getSilenceAnalysis().getPauseRatePercent();
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