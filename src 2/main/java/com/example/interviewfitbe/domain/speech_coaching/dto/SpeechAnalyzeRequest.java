package com.example.interviewfitbe.domain.speech_coaching.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

// 클라이언트 → Spring 스피치 분석 요청 DTO
@Getter
@Builder
@Jacksonized
public class SpeechAnalyzeRequest {

    private String sourceType;
    private String sourceValue;
    private String jobCategory;
    private Long interviewId;
    private Long interviewAnswerId;
    private Long questionId;
    private String originalQuestion;
}