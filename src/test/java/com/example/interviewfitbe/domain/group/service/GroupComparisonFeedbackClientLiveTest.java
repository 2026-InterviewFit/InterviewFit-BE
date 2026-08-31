package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupComparisonFeedbackRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupComparisonFeedbackClientLiveTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_GROUP_COMPARISON_LIVE_TEST", matches = "true")
    void callsRunningFastApiAndMapsParticipantFeedback() {
        GroupComparisonFeedbackClient client = new GroupComparisonFeedbackClient(
                WebClient.builder().baseUrl("http://127.0.0.1:8000").build());
        ReflectionTestUtils.setField(client, "enabled", true);

        var response = client.createFeedback(new GroupComparisonFeedbackRequest(
                1L, 0, List.of(
                participant(3L, "85", "76", "88", "80", "74", "82", 1),
                participant(4L, "72", "81", "70", "75", "86", "75", 2))));

        assertThat(response).extracting(GroupComparisonFeedbackClient.Feedback::participantUserId)
                .containsExactly(3L, 4L);
        assertThat(response).allSatisfy(feedback -> {
            assertThat(feedback.strengthSummary()).isNotBlank();
            assertThat(feedback.improvementSummary()).isNotBlank();
        });

        var overallResponse = client.createFeedback(new GroupComparisonFeedbackRequest(
                1L, null, List.of(
                participant(3L, "84", "78", "86", "82", "76", "82", 1),
                participant(4L, "74", "80", "72", "77", "84", "76", 2))));
        assertThat(overallResponse).extracting(GroupComparisonFeedbackClient.Feedback::participantUserId)
                .containsExactly(3L, 4L);
        assertThat(overallResponse).allSatisfy(feedback -> {
            assertThat(feedback.strengthSummary()).isNotBlank();
            assertThat(feedback.improvementSummary()).isNotBlank();
        });
    }

    private GroupComparisonFeedbackRequest.Participant participant(
            Long id, String content, String speech, String gaze, String pose,
            String blink, String total, int rank) {
        return new GroupComparisonFeedbackRequest.Participant(id,
                new BigDecimal(content), new BigDecimal(speech), new BigDecimal(gaze),
                new BigDecimal(pose), new BigDecimal(blink), new BigDecimal(total), rank);
    }
}
