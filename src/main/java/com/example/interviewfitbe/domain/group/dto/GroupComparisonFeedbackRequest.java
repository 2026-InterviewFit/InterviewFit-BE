package com.example.interviewfitbe.domain.group.dto;

import java.math.BigDecimal;
import java.util.List;

public record GroupComparisonFeedbackRequest(
        Long roomId,
        Integer questionIndex,
        List<Participant> participants
) {
    public record Participant(
            Long participantUserId,
            BigDecimal contentScore,
            BigDecimal speechScore,
            BigDecimal gazeScore,
            BigDecimal poseScore,
            BigDecimal blinkScore,
            BigDecimal totalScore,
            Integer rank
    ) {}
}
