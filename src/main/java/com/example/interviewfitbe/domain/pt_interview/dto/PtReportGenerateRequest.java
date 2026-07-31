package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * FastAPI PT 종합 리포트 생성 요청
 */
public record PtReportGenerateRequest(

        @JsonProperty("presentation_summary")
        String presentationSummary,

        @JsonProperty("questions")
        List<String> questions,

        @JsonProperty("answers")
        List<String> answers,

        @JsonProperty("eye_scores")
        List<Double> eyeScores,

        @JsonProperty("blink_scores")
        List<Double> blinkScores,

        @JsonProperty("pose_scores")
        List<Double> poseScores,

        @JsonProperty("speech_scores")
        List<Double> speechScores

) {
}
