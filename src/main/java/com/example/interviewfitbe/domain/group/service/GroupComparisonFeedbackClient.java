package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupComparisonFeedbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupComparisonFeedbackClient {
    private final WebClient fastApiWebClient;

    @Value("${app.group-interview.comparison-feedback-enabled:false}")
    private boolean enabled;

    public List<Feedback> createFeedback(GroupComparisonFeedbackRequest request) {
        if (!enabled) return Collections.emptyList();
        try {
            FeedbackResponse response = fastApiWebClient.post()
                    .uri("/apis/group-interviews/comparisons/feedback")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FeedbackResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            return response == null || response.participants() == null
                    ? Collections.emptyList()
                    : response.participants();
        } catch (Exception exception) {
            log.warn("그룹 상대 비교 피드백 생성을 건너뜁니다. roomId={}, questionIndex={}, error={}",
                    request.roomId(), request.questionIndex(), exception.getMessage());
            return Collections.emptyList();
        }
    }

    public record FeedbackResponse(List<Feedback> participants) {}
    public record Feedback(Long participantUserId, String strengthSummary, String improvementSummary) {}
}
