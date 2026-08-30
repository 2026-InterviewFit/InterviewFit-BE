package com.example.interviewfitbe.domain.group.entity;

public enum GroupQuestionType {
    INTRODUCTION,
    COMMON_COMPETENCY,
    PRESSURE,
    JOB_SPECIFIC;

    public static GroupQuestionType fromRound(int round) {
        return switch (round) {
            case 1 -> INTRODUCTION;
            case 2 -> COMMON_COMPETENCY;
            case 3 -> PRESSURE;
            case 4 -> JOB_SPECIFIC;
            default -> throw new IllegalArgumentException("지원하지 않는 면접 라운드입니다: " + round);
        };
    }
}
