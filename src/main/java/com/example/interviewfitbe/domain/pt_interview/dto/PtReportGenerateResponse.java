package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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

        @JsonProperty("presentation_material_summary")
        String presentationMaterialSummary,

        @JsonProperty("actual_presentation_summary")
        String actualPresentationSummary,

        @JsonProperty("presentation_comparison")
        String presentationComparison,

        @JsonProperty("question_analyses")
        List<PtQuestionAnalysisResponse> questionAnalyses,

        String strengths,

        String weaknesses,

        String improvement

) {
}