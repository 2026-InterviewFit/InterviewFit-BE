package com.example.interviewfitbe.domain.pt_interview.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;

public record PtAnswerUploadResponse(

        Long answerId,

        Long ptInterviewId,

        Long ptQuestionId,

        Integer questionNumber,

        String question,

        AnalysisStatus analysisStatus

) {
}