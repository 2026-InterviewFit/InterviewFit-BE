package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PtQuestionResult(

        @JsonProperty("question_number")
        Integer questionNumber,

        String question,

        String reason

) {
}