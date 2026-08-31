package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
import com.example.interviewfitbe.domain.followup.repository.FollowUpAnswerRepository;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.dto.InterviewReportResponse;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.enums.SpeechScoreStatus;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.service.ExternalAiAnalysisJsonCodec;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InterviewServiceSpeechReportTest {

    private final InterviewService service = new InterviewService(
            mock(UserRepository.class),
            mock(JobRoleRepository.class),
            mock(InterviewRepository.class),
            mock(InterviewAnswerRepository.class),
            mock(EyeBlinkingReportRepository.class),
            mock(EyeGazeReportRepository.class),
            mock(PoseReportRepository.class),
            mock(SpeechReportRepository.class),
            mock(FollowUpQuestionRepository.class),
            mock(FollowUpAnswerRepository.class),
            mock(AiEvaluationRepository.class),
            new ObjectMapper(),
            mock(ExternalAiAnalysisJsonCodec.class)
    );

    @Test
    void returnsStoredPythonSpeechScoreAndExistingMetrics() {
        SpeechReportEntity entity = SpeechReportEntity.builder()
                .id(7L)
                .score(82.0)
                .speechScoreStatus(SpeechScoreStatus.SCORED)
                .speechScoreReason(null)
                .textScript("답변 내용")
                .charactersPerSecond(3.2)
                .totalSilenceSeconds(1.5)
                .pauseRatePercent(12.5)
                .criticalSilenceCount(2)
                .totalFillerCount(3)
                .fillerDetailsJson("{\"음\":3}")
                .repeatedExpressionsJson("[{\"expression\":\"제가\"}]")
                .build();

        InterviewReportResponse.SpeechReport response = ReflectionTestUtils.invokeMethod(
                service, "toSpeechReport", entity, null);

        assertThat(response).isNotNull();
        assertThat(response.getSpeechScore()).isEqualTo(82.0);
        assertThat(response.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.SCORED);
        assertThat(response.getSpeechScoreReason()).isNull();
        assertThat(response.getCharactersPerSecond()).isEqualTo(3.2);
        assertThat(response.getTotalSilenceSeconds()).isEqualTo(1.5);
        assertThat(response.getPauseRatePercent()).isEqualTo(12.5);
        assertThat(response.getCriticalSilenceCount()).isEqualTo(2);
        assertThat(response.getTotalFillerCount()).isEqualTo(3);
        assertThat(response.getFillerDetailsJson()).isEqualTo("{\"음\":3}");
        assertThat(response.getRepeatedExpressionsJson()).isEqualTo("[{\"expression\":\"제가\"}]");
    }

    @Test
    void keepsInsufficientSpeechScoreNullWithStatusAndReason() {
        SpeechReportEntity entity = SpeechReportEntity.builder()
                .score(null)
                .speechScoreStatus(SpeechScoreStatus.INSUFFICIENT_DATA)
                .speechScoreReason("발화 데이터가 부족하여 점수를 산출할 수 없습니다.")
                .build();

        InterviewReportResponse.SpeechReport response = ReflectionTestUtils.invokeMethod(
                service, "toSpeechReport", entity, null);

        assertThat(response).isNotNull();
        assertThat(response.getSpeechScore()).isNull();
        assertThat(response.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.INSUFFICIENT_DATA);
        assertThat(response.getSpeechScoreReason())
                .isEqualTo("발화 데이터가 부족하여 점수를 산출할 수 없습니다.");
    }
}
