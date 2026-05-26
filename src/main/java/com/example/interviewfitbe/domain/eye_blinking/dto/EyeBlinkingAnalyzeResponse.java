package com.example.interviewfitbe.domain.eye_blinking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class EyeBlinkingAnalyzeResponse {
    private String message;
    private Long reportId;
    private Map<String, Object> result;
}