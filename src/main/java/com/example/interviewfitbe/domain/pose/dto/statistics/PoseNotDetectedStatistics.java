package com.example.interviewfitbe.domain.pose.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseNotDetectedStatistics {
    // Not Detected 전체 집계
    double notDetectedDuration;

    // 부위 별 Not Detected 집계
    double shoulderNotDetectedDuration;
    double faceNotDetectedDuration;
}
