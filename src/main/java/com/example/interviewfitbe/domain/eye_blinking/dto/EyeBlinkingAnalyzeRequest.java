package com.example.interviewfitbe.domain.eye_blinking.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EyeBlinkingAnalyzeRequest {
    private String videoUrl;
    private Long userId;
    private Long interviewId;
}