package com.example.interviewfitbe.domain.followup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FollowUpAnswerCreateRequest {

    private Long interviewId;

    @JsonProperty("follow_up_question_id")
    @JsonAlias("followUpQuestionId")
    private Long followUpQuestionId;

    @JsonProperty("answer_text")
    @JsonAlias({"answerText", "answer"})
    private String answerText;

    @JsonProperty("start_time")
    @JsonAlias("startTime")
    private Double startTime;

    @JsonProperty("end_time")
    @JsonAlias("endTime")
    private Double endTime;
}