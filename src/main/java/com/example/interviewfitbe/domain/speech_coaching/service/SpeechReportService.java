package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
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


@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechReportRepository speechReportRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final FollowUpService followUpService;

    @Transactional
    public Long saveReport(SpeechReportRequest request) {
        try {
            // 1. 요청에 담긴 interviewId로 면접 세션 찾기
            InterviewEntity interview = interviewRepository.findById(request.getInterviewId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 면접 정보를 찾을 수 없습니다. id=" + request.getInterviewId()));

            // 2. 리포트 엔티티 빌드
            SpeechReportEntity report = SpeechReportEntity.builder()
                    .interview(interview)
                    .textScript(request.getTextScript())
                    .totalFillerCount(request.getFillerWordAnalysis().getTotalCount())
                    .fillerDetailsJson(objectMapper.writeValueAsString(request.getFillerWordAnalysis().getDetails()))
                    .paceEvaluation(request.getSpeechPaceAnalysis().getEvaluation())
                    .paceFeedback(request.getSpeechPaceAnalysis().getFeedback())
                    .charactersPerSecond(request.getSpeechPaceAnalysis().getCharactersPerSecond())
                    .totalSilenceSeconds(request.getSilenceAnalysis().getTotalSilenceSeconds())
                    .criticalSilenceCount(request.getSilenceAnalysis().getCriticalSilenceCount())
                    .aiEvaluationJson(objectMapper.writeValueAsString(request.getAiEvaluation()))
                    .modelAnswer(request.getModelAnswer())
                    .build();

            SpeechReportEntity saved = speechReportRepository.save(report);
            log.info("스피치 리포트 DB 저장 완료 - id: {}, interviewId: {}", saved.getId(), interview.getId());

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

            return saved.getId();

        }catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("리포트 데이터 변환 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public SpeechReportEntity getReport(Long id) {
        return speechReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리포트를 찾을 수 없습니다. id=" + id));
    }


    /**
     * speech의 textScript를 followup의 answer로 사용한다.
     *
     * followup 생성에 필요한 최소 조건:
     * - answer: request.textScript
     * - originalQuestion 또는 questionId
     * - job/interviewId/jobRoleId/questionId 중 하나로 직무 추론 가능
     */
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