package com.example.interviewfitbe.domain.followup.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FollowUpAnswerCreateResponse {

    private String message;

    @JsonProperty("follow_up_answer_id")
    private Long followUpAnswerId;

    private Long interviewId;

    @JsonProperty("follow_up_question_id")
    private Long followUpQuestionId;

    @JsonProperty("answer_text")
    private String answerText;

    @JsonProperty("start_time")
    private Double startTime;

    @JsonProperty("end_time")
    private Double endTime;

    private AnalysisStatus analysisStatus;
}