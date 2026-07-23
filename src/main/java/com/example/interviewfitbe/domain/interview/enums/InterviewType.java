package com.example.interviewfitbe.domain.interview.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InterviewType {

    ONE_ON_ONE("1:1 면접"),
    MANY_TO_ONE("다대일 면접"),
    PT("PT 발표 면접");

    private final String displayName;
}