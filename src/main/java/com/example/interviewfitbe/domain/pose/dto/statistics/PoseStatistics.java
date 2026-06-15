package com.example.interviewfitbe.domain.pose.dto.statistics;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class PoseStatistics {
    // 전체 집계
    int badCount;
    int badDuration;

    // BAD
    int shoulderBadCount;
    int faceBadCount;

    int shoulderBadDuration;
    int faceBadDuration;
}
