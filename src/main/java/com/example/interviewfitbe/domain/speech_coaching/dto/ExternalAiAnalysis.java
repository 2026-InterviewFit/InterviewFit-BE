package com.example.interviewfitbe.domain.speech_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAiAnalysis {

    private Star star;
    private List<String> strengths;
    private List<String> improvements;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Star {
        private String situation;
        private String task;
        private String action;
        private String result;
    }
}
