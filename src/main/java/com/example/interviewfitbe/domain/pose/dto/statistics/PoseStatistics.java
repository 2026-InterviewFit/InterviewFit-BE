package com.example.interviewfitbe.domain.pose.dto.statistics;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseStatistics {
    // BAD 전체 집계
    int badCount;
    double badDuration;

    // 부위 별 BAD 집계
    int shoulderBadCount;
    int faceBadCount;
    double shoulderBadDuration;
    double faceBadDuration;
}
