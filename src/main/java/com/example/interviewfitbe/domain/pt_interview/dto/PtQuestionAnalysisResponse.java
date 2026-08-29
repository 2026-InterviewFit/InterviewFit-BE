package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PtQuestionAnalysisResponse(

        @JsonProperty("question_number")
        Integer questionNumber,

        String question,

        String answer,

        @JsonProperty("understanding_score")
        Integer understandingScore,

        @JsonProperty("logic_score")
        Integer logicScore,

        @JsonProperty("specificity_score")
        Integer specificityScore,

        @JsonProperty("evidence_score")
        Integer evidenceScore,

        @JsonProperty("overall_score")
        Integer overallScore,

        String strengths,

        String weaknesses,

        String improvement,

        @JsonProperty("improved_answer")
        String improvedAnswer

) {
}