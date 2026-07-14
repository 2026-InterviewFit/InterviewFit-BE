package com.example.interviewfitbe.domain.pose.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseAnalysisResultDto {
    private Long id;
    private Long interviewId;
    private Long interviewAnswerId;
    private Double totalDuration;
    private Integer badCount;
    private Double badDuration;
    private Integer shoulderBadCount;
    private Integer faceBadCount;
    private Double shoulderBadDuration;
    private Double faceBadDuration;
    private Double notDetectedDuration;
    private Double shoulderNotDetectedDuration;
    private Double faceNotDetectedDuration;
    private Double badRatio;
    private Integer score;
    private String grade;
    private String createdAt;
}
