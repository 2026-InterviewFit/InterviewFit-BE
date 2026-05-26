package com.example.interviewfitbe.domain.followup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowUpCreateResponse {

    private String message;

    private Long followUpId;

    @JsonProperty("follow_up_question")
    private String followUpQuestion;
}