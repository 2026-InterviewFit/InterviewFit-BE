package com.example.interviewfitbe.domain.interview.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InterviewStatus {

    CREATED("생성됨"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    CANCELED("취소됨");

    private final String displayName;
}