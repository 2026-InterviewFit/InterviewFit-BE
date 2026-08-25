package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupComparisonFeedbackRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupComparisonFeedbackClientTest {

    @Test
    void disabledFeedbackReturnsEmptyWithoutAffectingNumericComparison() {
        GroupComparisonFeedbackClient client = client(false);

        assertThat(client.createFeedback(request())).isEmpty();
    }

    @Test
    void fastApiFailureIsFailSoft() {
        GroupComparisonFeedbackClient client = client(true);

        assertThat(client.createFeedback(request())).isEmpty();
    }

    private GroupComparisonFeedbackClient client(boolean enabled) {
        GroupComparisonFeedbackClient client = new GroupComparisonFeedbackClient(
                WebClient.builder().baseUrl("http://127.0.0.1:1").build());
        ReflectionTestUtils.setField(client, "enabled", enabled);
        return client;
    }

    private GroupComparisonFeedbackRequest request() {
        return new GroupComparisonFeedbackRequest(1L, 0, List.of(
                participant(1L, 82, 1), participant(2L, 75, 2)));
    }

    private GroupComparisonFeedbackRequest.Participant participant(Long id, int total, int rank) {
        BigDecimal score = BigDecimal.valueOf(total);
        return new GroupComparisonFeedbackRequest.Participant(
                id, score, score, score, score, score, score, rank);
    }
}
