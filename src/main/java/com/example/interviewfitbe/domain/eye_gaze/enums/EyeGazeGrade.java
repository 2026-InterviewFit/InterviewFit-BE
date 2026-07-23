package com.example.interviewfitbe.domain.eye_gaze.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EyeGazeGrade {

    EXCELLENT("A+", "매우 안정적"),
    GOOD("A", "안정적"),
    AVERAGE("B", "보통"),
    NEEDS_IMPROVEMENT("C", "개선 필요"),
    POOR("D", "불안정");

    private final String label;
    private final String displayName;
}
