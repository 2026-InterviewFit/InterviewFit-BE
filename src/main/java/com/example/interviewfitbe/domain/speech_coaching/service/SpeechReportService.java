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


@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechReportRepository speechReportRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;

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
                    .build();

            SpeechReportEntity saved = speechReportRepository.save(report);
            log.info("스피치 리포트 DB 저장 완료 - id: {}, interviewId: {}", saved.getId(), interview.getId());
            return saved.getId();

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("리포트 데이터 변환 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public SpeechReportEntity getReport(Long id) {
        return speechReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리포트를 찾을 수 없습니다. id=" + id));
    }
}