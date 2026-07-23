package com.example.interviewfitbe.domain.followup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FollowUpCreateResponse {

    private String message;

    @JsonProperty("follow_up_question_id")
    private Long followUpQuestionId;

    private Long interviewId;

    @JsonProperty("source_question_id")
    private Long sourceQuestionId;

    @JsonProperty("source_answer_id")
    private Long sourceAnswerId;

    private String job;

    @JsonProperty("original_question")
    private String originalQuestion;

    private String answer;

    @JsonProperty("follow_up_question")
    private String followUpQuestion;

    @JsonProperty("interview_style")
    private String interviewStyle;
}