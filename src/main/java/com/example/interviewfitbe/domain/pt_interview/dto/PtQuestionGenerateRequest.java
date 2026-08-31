package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PtQuestionGenerateRequest(

        @JsonProperty("document_summary")
        String documentSummary,

        @JsonProperty("slides_text")
        String slidesText,

        @JsonProperty("question_count")
        Integer questionCount

) {
}
