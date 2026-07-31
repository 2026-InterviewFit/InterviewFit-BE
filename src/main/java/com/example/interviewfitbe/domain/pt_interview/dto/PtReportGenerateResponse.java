package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FastAPI PT 종합 리포트 생성 응답
 */
public record PtReportGenerateResponse(

        @JsonProperty("content_score")
        Integer contentScore,

        @JsonProperty("structure_score")
        Integer structureScore,

        @JsonProperty("delivery_score")
        Integer deliveryScore,

        @JsonProperty("qa_score")
        Integer qaScore,

        @JsonProperty("overall_score")
        Integer overallScore,

        @JsonProperty("presentation_summary")
        String presentationSummary,

        String strengths,

        String weaknesses,

        String improvement

) {
}