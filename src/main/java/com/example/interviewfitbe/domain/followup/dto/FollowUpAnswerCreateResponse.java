package com.example.interviewfitbe.domain.followup.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FollowUpAnswerCreateResponse {

    private String message;

    private Long followUpAnswerId;

    private Long followUpQuestionId;

    private Long interviewId;

    private String answerText;

    private Double startTime;

    private Double endTime;

    private AnalysisStatus analysisStatus;
}