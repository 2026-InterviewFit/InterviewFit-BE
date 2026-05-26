package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechReportRequest;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReport;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import tools.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public Long saveReport(SpeechReportRequest request) {
        SpeechReport report = SpeechReport.builder()
                .textScript(request.getTextScript())
                .jobCategory(request.getJobCategory())
                .totalFillerCount(request.getFillerWordAnalysis().getTotalCount())
                .fillerDetailsJson(
                        objectMapper.writeValueAsString(request.getFillerWordAnalysis().getDetails())
                )
                .paceEvaluation(request.getSpeechPaceAnalysis().getEvaluation())
                .paceFeedback(request.getSpeechPaceAnalysis().getFeedback())
                .charactersPerSecond(request.getSpeechPaceAnalysis().getCharactersPerSecond())
                .totalSilenceSeconds(request.getSilenceAnalysis().getTotalSilenceSeconds())
                .criticalSilenceCount(request.getSilenceAnalysis().getCriticalSilenceCount())
                .aiEvaluationJson(
                        objectMapper.writeValueAsString(request.getAiEvaluation())
                )
                .build();

        SpeechReport saved = speechReportRepository.save(report);
        log.info("스피치 리포트 DB 저장 완료 - id: {}", saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public SpeechReport getReport(Long id) {
        return speechReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("리포트를 찾을 수 없습니다. id=" + id));
    }
}