package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechReportRequest;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.enums.SpeechScoreStatus;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeechReportScorePersistenceTest {

    private SpeechReportRepository reportRepository;
    private SpeechReportService service;

    @BeforeEach
    void setUp() {
        reportRepository = mock(SpeechReportRepository.class);
        InterviewRepository interviewRepository = mock(InterviewRepository.class);
        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(InterviewEntity.builder().id(10L).build()));
        when(reportRepository.findByInterview_Id(10L)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new SpeechReportService(
                reportRepository,
                mock(AiEvaluationRepository.class),
                new ExternalAiAnalysisJsonCodec(new ObjectMapper()),
                interviewRepository,
                mock(InterviewAnswerRepository.class),
                new ObjectMapper(),
                mock(SpeechAnalysisService.class)
        );
    }

    @Test
    void persistsActualZeroScore() {
        service.saveReport(request(0, SpeechScoreStatus.SCORED, null));

        SpeechReportEntity saved = capturedReport();
        assertThat(saved.getScore()).isEqualTo(0.0);
        assertThat(saved.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.SCORED);
        assertThat(saved.getSpeechScoreReason()).isNull();
    }

    @Test
    void persistsInsufficientDataAsNullInsteadOfZero() {
        service.saveReport(request(
                null,
                SpeechScoreStatus.INSUFFICIENT_DATA,
                "발화 데이터가 부족하여 점수를 산출할 수 없습니다."
        ));

        SpeechReportEntity saved = capturedReport();
        assertThat(saved.getScore()).isNull();
        assertThat(saved.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.INSUFFICIENT_DATA);
        assertThat(saved.getSpeechScoreReason()).contains("발화 데이터가 부족");
    }

    private SpeechReportRequest request(
            Integer score,
            SpeechScoreStatus status,
            String reason
    ) {
        return SpeechReportRequest.builder()
                .interviewId(10L)
                .speechScore(score)
                .speechScoreStatus(status)
                .speechScoreReason(reason)
                .build();
    }

    private SpeechReportEntity capturedReport() {
        ArgumentCaptor<SpeechReportEntity> captor = ArgumentCaptor.forClass(SpeechReportEntity.class);
        verify(reportRepository).save(captor.capture());
        return captor.getValue();
    }
}
