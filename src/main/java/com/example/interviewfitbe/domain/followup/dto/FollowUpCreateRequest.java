package com.example.interviewfitbe.domain.followup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /**
     * 원본 질문 ID.
     */
    private Long questionId;

    /**
     * 원본 답변 ID (있으면 answer 텍스트 자동 조회).
     */
    private Long sourceAnswerId;

    @JsonProperty("original_question")
    @JsonAlias({"originalQuestion", "question"})
    private String originalQuestion;

    /**
     * 답변 텍스트.
     * sourceAnswerId가 있으면 생략 가능 (DB에서 answerText 조회).
     */
    private String answer;

    @JsonProperty("interview_style")
    @JsonAlias("interviewStyle")
    private String interviewStyle = "normal";

    private Long userId;

    private Long interviewId;
}