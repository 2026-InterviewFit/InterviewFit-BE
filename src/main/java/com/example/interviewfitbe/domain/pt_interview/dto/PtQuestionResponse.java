package com.example.interviewfitbe.domain.pt_interview.dto;

public record PtQuestionResponse(

        Long id,

        Integer questionNumber,

        String question,

        String reason

) {
}
