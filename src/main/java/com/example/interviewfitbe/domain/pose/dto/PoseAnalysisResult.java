package com.example.interviewfitbe.domain.pose.dto;

import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseAnalysisResult {
    private Double totalDuration;

    // 전체 BAD
    private Integer badCount;
    private Double badDuration;

    // 부위별 BAD 횟수
    private Integer shoulderBadCount;
    private Integer faceBadCount;

    // 부위별 BAD 지속 시간
    private Double shoulderBadDuration;
    private Double faceBadDuration;

    // 전체 Not Detected 지속 시간
    private Double notDetectedDuration;
    // 부위별 Not Detected 지속 시간
    private Double shoulderNotDetectedDuration;
    private Double faceNotDetectedDuration;

    // BAD 비율
    private Double badRatio;
    // 점수
    private Integer score;
    // 등급
    private PoseGrade grade;
}
