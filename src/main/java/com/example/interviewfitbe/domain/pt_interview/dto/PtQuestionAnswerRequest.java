package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PT 질문과 사용자의 실제 답변
 */
public record PtQuestionAnswerRequest(

        @JsonProperty("question_number")
        Integer questionNumber,

        String question,

        String answer

) {
}
