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
        String improvedAnswer,

        /*
         * 기존 질문 답변 AI 평가에서 가져오는 상세 데이터
         */
        String aiFeedback,

        String modelAnswer,

        PtStarAnalysisResponse starAnalysis

) {
}