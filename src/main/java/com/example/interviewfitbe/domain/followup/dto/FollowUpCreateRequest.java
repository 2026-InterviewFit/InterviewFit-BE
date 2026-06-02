package com.example.interviewfitbe.domain.followup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FollowUpCreateRequest {

    private Long jobRoleId;

    private String job;

    @JsonProperty("original_question")
    private String originalQuestion;

    private String answer;

    @JsonProperty("interview_style")
    private String interviewStyle = "normal";

    private Long userId;

    private Long interviewId;
}