package com.example.interviewfitbe.domain.interview.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InterviewAnswerUploadResponse {

    private Long answerId;

    private Long interviewId;

    private Long questionId;

    private String questionText;

    private String videoObjectKey;

    private Integer answerOrder;

    private AnalysisStatus analysisStatus;
}