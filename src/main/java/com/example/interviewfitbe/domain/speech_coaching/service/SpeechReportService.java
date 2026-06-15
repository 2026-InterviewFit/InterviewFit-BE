package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
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
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.service.FollowUpService;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechReportRepository speechReportRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final FollowUpService followUpService;

    // ✅ 추가: FastAPI 호출 전담 서비스 주입
    private final SpeechAnalysisService speechAnalysisService;

    /**
     * ✅ 신규: 클라이언트 요청 → FastAPI 분석 → DB 저장
     *
     * EyeBlinkingService.analyzeAndSave()와 동일한 패턴.
     *
     * 흐름:
     * SpeechReportController
     * → analyzeAndSave(SpeechAnalyzeRequest)
     * → SpeechAnalysisService.analyzeSpeech() → FastAPI 호출
     * → FastAPI 응답을 SpeechReportRequest로 변환
     * → saveReport()로 DB 저장
     */
    @Transactional
    public Long analyzeAndSave(SpeechAnalyzeRequest request) {
        // 1. FastAPI 호출 (Spring이 직접 호출)
        Map<String, Object> fastApiResponse = speechAnalysisService.analyzeSpeech(
                request.getSourceType(),
                request.getSourceValue(),
                request.getJobCategory(),
                request.getInterviewId()
        );

        // 2. FastAPI 응답 Map → SpeechReportRequest 변환
        //    SpeechReportRequest의 @JsonProperty 어노테이션 덕분에
        //    snake_case 키도 자동으로 매핑됨
        SpeechReportRequest reportRequest = objectMapper.convertValue(
                fastApiResponse, SpeechReportRequest.class
        );

        // 3. interviewId를 request에서 확실히 세팅 (FastAPI 응답에 없을 경우 대비)
        if (reportRequest.getInterviewId() == null) {
            reportRequest.setInterviewId(request.getInterviewId());
        }

        // 4. 기존 saveReport() 재사용
        return saveReport(reportRequest);
    }

    /**
     * 기존 유지: FastAPI에서 직접 받던 리포트 저장 로직.
     * analyzeAndSave()에서 내부적으로 재사용함.
     */
    @Transactional
    public Long saveReport(SpeechReportRequest request) {
        try {
            // 1. 요청에 담긴 interviewId로 면접 세션 찾기
            InterviewEntity interview = interviewRepository.findById(request.getInterviewId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "해당 면접 정보를 찾을 수 없습니다. id=" + request.getInterviewId()
                    ));

            // 2. 리포트 엔티티 빌드
            SpeechReportEntity report = SpeechReportEntity.builder()
                    .interview(interview)
                    .textScript(request.getTextScript())
                    .totalFillerCount(request.getFillerWordAnalysis().getTotalCount())
                    .fillerDetailsJson(objectMapper.writeValueAsString(
                            request.getFillerWordAnalysis().getDetails()))
                    .paceEvaluation(request.getSpeechPaceAnalysis().getEvaluation())
                    .paceFeedback(request.getSpeechPaceAnalysis().getFeedback())
                    .charactersPerSecond(request.getSpeechPaceAnalysis().getCharactersPerSecond())
                    .totalSilenceSeconds(request.getSilenceAnalysis().getTotalSilenceSeconds())
                    .criticalSilenceCount(request.getSilenceAnalysis().getCriticalSilenceCount())
                    .aiEvaluationJson(objectMapper.writeValueAsString(request.getAiEvaluation()))
                    .modelAnswer(request.getModelAnswer())
                    .build();

            SpeechReportEntity saved = speechReportRepository.save(report);
            log.info("스피치 리포트 DB 저장 완료 - id: {}, interviewId: {}",
                    saved.getId(), interview.getId());

            // 3. 꼬리질문 생성 시도 (실패해도 리포트 저장은 유지)
            try {
                FollowUpCreateResponse followUpResponse = createFollowUpIfPossible(request);

                if (followUpResponse != null) {
                    log.info("스피치 텍스트 기반 꼬리질문 생성 완료 - followUpId: {}, interviewId: {}",
                            followUpResponse.getFollowUpId(), request.getInterviewId());
                }
            } catch (Exception e) {
                log.warn("스피치 리포트 저장은 완료됐지만 꼬리질문 생성은 실패했습니다. interviewId={}, reason={}",
                        request.getInterviewId(), e.getMessage());
            }

            return saved.getId();

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("리포트 데이터 변환 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public SpeechReportEntity getReport(Long id) {
        return speechReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "리포트를 찾을 수 없습니다. id=" + id
                ));
    }

    private FollowUpCreateResponse createFollowUpIfPossible(SpeechReportRequest request) {
        if (request.getTextScript() == null || request.getTextScript().isBlank()) {
            log.warn("textScript가 비어 있어 꼬리질문 생성을 건너뜁니다.");
            return null;
        }

        boolean hasQuestion =
                request.getQuestionId() != null ||
                        (request.getOriginalQuestion() != null && !request.getOriginalQuestion().isBlank());

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
}
