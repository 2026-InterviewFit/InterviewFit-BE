package com.example.interviewfitbe.domain.pose.calculator;

import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import org.springframework.stereotype.Component;

@Component
public class PoseScoreCalculator {
    public PoseScoreResult calculateScoreResult(double badDuration, double notDetectedDuration, double totalDuration) {
        if (totalDuration <= 0) {
            return PoseScoreResult.builder()
                    .badRatio(0.0)
                    .score(0)
                    .grade(PoseGrade.E)
                    .build();
        }

        double badRatio = Math.min(1.0, badDuration / totalDuration);
        int score = calculateBaseScore(badRatio);

        double notDetectedRatio = notDetectedDuration / totalDuration;
        if (notDetectedRatio >= 0.3) {
            score = applyNotDetectedPenalty(score, notDetectedRatio);
        }

        PoseGrade grade = calculateGrade(score);

        return PoseScoreResult.builder()
                .badRatio(badRatio)
                .score(score)
                .grade(grade)
                .build();
    }

    public int calculateBaseScore(double badRatio) {
        return Math.max(0, 100 - (int)(badRatio * 100));
    }

    public int applyNotDetectedPenalty(int score, double notDetectedRatio) {
        int penalty = (int) ((notDetectedRatio - 0.3) / 0.7 * 100);
        return Math.max(0, score - penalty);
    }

    public PoseGrade calculateGrade(int score) {
        if (score >= 85) return PoseGrade.A;
        else if (score >= 75) return PoseGrade.B;
        else if (score >= 65) return PoseGrade.C;
        else if (score >= 50) return PoseGrade.D;
        return PoseGrade.E;
    }
}
