package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FastAPI PT 종합 리포트 생성 응답
 */
public record PtReportGenerateResponse(

        @JsonProperty("content_score")
        Double contentScore,

        @JsonProperty("structure_score")
        Double structureScore,

        @JsonProperty("delivery_score")
        Double deliveryScore,

        @JsonProperty("qa_score")
        Double qaScore,

        @JsonProperty("overall_score")
        Double overallScore,

        @JsonProperty("presentation_summary")
        String presentationSummary,

        String strengths,

        String weaknesses,

        String improvement

) {
}
