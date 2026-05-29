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
public class PoseAnalysisResultDto {
    private Long id;
    private Integer totalDuration;
    private Integer badCount;
    private Integer badDuration;
    private Integer shoulderBadCount;
    private Integer faceBadCount;
    private Integer shoulderBadDuration;
    private Integer faceBadDuration;
    private Double badRatio;
    private Integer score;
    private PoseGrade grade;
}
