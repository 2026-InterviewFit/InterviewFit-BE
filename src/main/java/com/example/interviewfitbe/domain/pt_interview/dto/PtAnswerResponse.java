package com.example.interviewfitbe.domain.pt_interview.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;

import java.time.LocalDateTime;

public record PtAnswerResponse(

        Long answerId,

        Long ptQuestionId,

        Integer questionNumber,

        String question,

        String answerText,

        AnalysisStatus analysisStatus,

        LocalDateTime createdAt

) {
}
