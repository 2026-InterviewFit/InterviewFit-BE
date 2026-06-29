package com.example.interviewfitbe.domain.speech_coaching.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@Jacksonized
@ToString
public class SpeechReportRequest {

    @JsonProperty("interview_id")
    @JsonAlias("interviewId")
    private Long interviewId;

    @JsonProperty("interview_answer_id")
    @JsonAlias("interviewAnswerId")
    private Long interviewAnswerId;

    @JsonProperty("question_id")
    @JsonAlias("questionId")
    private Long questionId;

    @JsonProperty("original_question")
    @JsonAlias({"originalQuestion", "question"})
    private String originalQuestion;

    @JsonProperty("job_category")
    @JsonAlias({"jobCategory", "job"})
    private String jobCategory;

    @JsonProperty("text_script")
    @JsonAlias("textScript")
    private String textScript;

    @JsonProperty("filler_word_analysis")
    private FillerWordAnalysis fillerWordAnalysis;

    @JsonProperty("repeated_expressions")
    private List<RepeatedExpression> repeatedExpressions;

    @JsonProperty("speech_pace_analysis")
    private SpeechPaceAnalysis speechPaceAnalysis;

    @JsonProperty("volume_analysis")
    private VolumeAnalysis volumeAnalysis;

    @JsonProperty("silence_analysis")
    private SilenceAnalysis silenceAnalysis;

    @JsonProperty("ai_evaluation")
    private Map<String, Object> aiEvaluation;

    @JsonProperty("model_answer")
    private String modelAnswer;

    // 필러워드 디테일
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class FillerWordAnalysis {
        private Map<String, Integer> details;

        @JsonProperty("total_count")
        private Integer totalCount;
    }

    // 반복 표현 단건
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class RepeatedExpression {
        private String expression;
        private Integer count;
    }

    // 말하기 속도
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class SpeechPaceAnalysis {
        private String evaluation;
        private String feedback;

        @JsonProperty("characters_per_second")
        private Double charactersPerSecond;
    }

    // 음량 평균/표준편차
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class VolumeAnalysis {
        @JsonProperty("volume_db_mean")
        private Double volumeDbMean;

        @JsonProperty("volume_db_std")
        private Double volumeDbStd;
    }

    // 침묵 구간 및 침묵 비율
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class SilenceAnalysis {
        @JsonProperty("total_silence_seconds")
        private Double totalSilenceSeconds;

        @JsonProperty("pause_rate_percent")
        private Double pauseRatePercent;

        @JsonProperty("critical_silence_count")
        private Integer criticalSilenceCount;
    }
}