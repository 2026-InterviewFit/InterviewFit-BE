package com.example.interviewfitbe.domain.eye_blinking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 눈 깜빡임 분석 등급.
 * 다른 분석 도메인(pose, eye_gaze 등)과는 별개로 관리한다.
 */
@Getter
@AllArgsConstructor
public enum EyeBlinkingGrade {

    EXCELLENT("A+", "매우 안정적"),
    GOOD("A", "안정적"),
    AVERAGE("B", "보통"),
    NEEDS_IMPROVEMENT("C", "개선 필요"),
    POOR("D", "불안정");

    private final String label;
    private final String displayName;

    /**
     * 0~100 점수를 등급으로 변환한다.
     */
    public static EyeBlinkingGrade fromScore(Double score) {
        if (score == null) {
            return null;
        }
        if (score >= 90) return EXCELLENT;
        if (score >= 80) return GOOD;
        if (score >= 70) return AVERAGE;
        if (score >= 60) return NEEDS_IMPROVEMENT;
        return POOR;
    }
}