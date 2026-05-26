// package com.example.interviewfitbe.domain.speech_coaching.dto;
package com.example.interviewfitbe.domain.speech_coaching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SpeechReportRequest {

    @JsonProperty("text_script")
    private String textScript;

    @JsonProperty("filler_word_analysis")
    private FillerWordAnalysis fillerWordAnalysis;

    @JsonProperty("speech_pace_analysis")
    private SpeechPaceAnalysis speechPaceAnalysis;

    @JsonProperty("silence_analysis")
    private SilenceAnalysis silenceAnalysis;

    // Gemini 결과는 성공/실패 구조가 달라서 Map으로 유연하게 받음
    @JsonProperty("ai_evaluation")
    private Map<String, Object> aiEvaluation;

    @JsonProperty("job_category")
    private String jobCategory;

    @Getter @Setter @NoArgsConstructor @ToString
    public static class FillerWordAnalysis {
        private Map<String, Integer> details;       // {"음": 3, "어": 2, ...}
        @JsonProperty("total_count")
        private Integer totalCount;
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class SpeechPaceAnalysis {
        private String evaluation;                  // "적절함" / "너무 느림" / "너무 빠름"
        private String feedback;
        @JsonProperty("characters_per_second")
        private Double charactersPerSecond;
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class SilenceAnalysis {
        @JsonProperty("total_silence_seconds")
        private Double totalSilenceSeconds;
        @JsonProperty("critical_silence_count")
        private Integer criticalSilenceCount;
    }
}