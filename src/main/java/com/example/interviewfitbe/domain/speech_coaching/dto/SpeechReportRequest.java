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

    /**
     * 면접 ID
     */
    @JsonProperty("interview_id")
    @JsonAlias("interviewId")
    private Long interviewId;


    /**
     * 질문 답변 ID
     * AiEvaluation 저장용
     */
    @JsonProperty("interview_answer_id")
    @JsonAlias("interviewAnswerId")
    private Long interviewAnswerId;


    /**
     * 질문 ID
     */
    @JsonProperty("question_id")
    @JsonAlias("questionId")
    private Long questionId;


    /**
     * 원본 질문
     */
    @JsonProperty("original_question")
    @JsonAlias({"originalQuestion", "question"})
    private String originalQuestion;


    /**
     * 직무
     */
    @JsonProperty("job_category")
    @JsonAlias({"jobCategory", "job"})
    private String jobCategory;


    /**
     * 전체 STT 결과
     */
    @JsonProperty("text_script")
    @JsonAlias("textScript")
    private String textScript;


    /**
     * 음성 종합 점수
     */
    @JsonProperty("speech_score")
    @JsonAlias("speechScore")
    private Integer speechScore;


    /**
     * 음성 점수 상태
     */
    @JsonProperty("speech_score_status")
    @JsonAlias("speechScoreStatus")
    private String speechScoreStatus;


    /**
     * 음성 점수 산출 사유
     */
    @JsonProperty("speech_score_reason")
    @JsonAlias("speechScoreReason")
    private String speechScoreReason;


    /**
     * 필러 분석
     */
    @JsonProperty("filler_word_analysis")
    private FillerWordAnalysis fillerWordAnalysis;


    /**
     * 반복 표현 분석
     */
    @JsonProperty("repeated_expressions")
    private List<RepeatedExpression> repeatedExpressions;


    /**
     * 말하기 속도 분석
     */
    @JsonProperty("speech_pace_analysis")
    private SpeechPaceAnalysis speechPaceAnalysis;


    /**
     * 침묵 분석
     */
    @JsonProperty("silence_analysis")
    private SilenceAnalysis silenceAnalysis;


    /**
     * 질문별 AI 평가
     */
    @JsonProperty("ai_evaluation")
    private AiEvaluation aiEvaluation;


    /**
     * 외부 학습 AI의 구조화된 답변 분석. 분석 실패 시 null이다.
     */
    @JsonProperty("external_ai_analysis")
    private ExternalAiAnalysis externalAiAnalysis;


    // ============================
    // 내부 DTO
    // ============================


    /**
     * 필러 단어 분석
     */
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class FillerWordAnalysis {

        private Map<String, Integer> details;

        @JsonProperty("total_count")
        private Integer totalCount;
    }


    /**
     * 반복 표현 분석
     */
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class RepeatedExpression {

        private String expression;

        private Integer count;
    }


    /**
     * 말하기 속도 분석
     */
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


    /**
     * 침묵 분석
     */
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


    /**
     * AI 평가
     * AiEvaluationEntity 저장용
     */
    @Getter
    @Builder
    @Jacksonized
    @ToString
    public static class AiEvaluation {

        /**
         * STAR 점수
         */
        @JsonProperty("star_score")
        private Map<String, Integer> starScore;

        /**
         * Gemini 피드백
         */
        private String feedback;

        /**
         * 핵심 키워드
         */
        private List<String> keywords;

        /**
         * AI 모범 답안
         */
        @JsonProperty("model_answer")
        private String modelAnswer;
    }
}
