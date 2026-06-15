package com.example.interviewfitbe.domain.pose.dto.statistics;

import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseScoreResult {
    private double badRatio;
    private int score;
    private PoseGrade grade;
}
