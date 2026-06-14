package com.example.interviewfitbe.domain.speech_coaching.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SpeechReportRequest {

    @JsonProperty("interview_id")
    @JsonAlias("interviewId")
    private Long interviewId;

    /**
     * followup 연결용 질문 ID.
     * Python/FE에서 question_id를 넘길 수 있으면 이 값을 사용한다.
     */
    @JsonProperty("question_id")
    @JsonAlias("questionId")
    private Long questionId;

    /**
     * followup 생성용 원본 질문.
     * questionId가 없을 때 이 값을 사용한다.
     */
    @JsonProperty("original_question")
    @JsonAlias({"originalQuestion", "question"})
    private String originalQuestion;

    /**
     * followup 생성용 직무명.
     * interviewId로 jobRole을 찾을 수 있으면 없어도 되지만,
     * 단독 테스트나 fallback용으로 받는다.
     */
    @JsonProperty("job_category")
    @JsonAlias({"jobCategory", "job"})
    private String jobCategory;

    /**
     * STT 결과 텍스트.
     * followup의 answer로 사용된다.
     */
    @JsonProperty("text_script")
    @JsonAlias("textScript")
    private String textScript;

    @JsonProperty("filler_word_analysis")
    private FillerWordAnalysis fillerWordAnalysis;

    @JsonProperty("speech_pace_analysis")
    private SpeechPaceAnalysis speechPaceAnalysis;

    @JsonProperty("silence_analysis")
    private SilenceAnalysis silenceAnalysis;

    // Gemini 평가 결과
    @JsonProperty("ai_evaluation")
    private Map<String, Object> aiEvaluation;

    // 적정답안 예시
    @JsonProperty("model_answer")
    private String modelAnswer;


    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class FillerWordAnalysis {
        private Map<String, Integer> details; // {"음": 3, "어": 2, ...}

        @JsonProperty("total_count")
        private Integer totalCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class SpeechPaceAnalysis {
        private String evaluation; // "적절함" / "너무 느림" / "너무 빠름"
        private String feedback;

        @JsonProperty("characters_per_second")
        private Double charactersPerSecond;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class SilenceAnalysis {
        @JsonProperty("total_silence_seconds")
        private Double totalSilenceSeconds;

        @JsonProperty("critical_silence_count")
        private Integer criticalSilenceCount;
    }
}