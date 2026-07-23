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

    private Long interviewId;

    /*
     * source_question_id.
     * FE에서 questionId로 보내도 받게 alias 처리.
     */
    @JsonProperty("source_question_id")
    @JsonAlias({"sourceQuestionId", "questionId"})
    private Long sourceQuestionId;

    /*
     * source_answer_id.
     */
    @JsonProperty("source_answer_id")
    @JsonAlias({"sourceAnswerId", "answerId"})
    private Long sourceAnswerId;

    private Long jobRoleId;

    private String job;

    @JsonProperty("original_question")
    @JsonAlias({"originalQuestion", "question"})
    private String originalQuestion;

    /*
     * 원 답변 텍스트.
     * sourceAnswerId가 있으면 DB의 answerText로 대체 가능.
     */
    private String answer;

    @JsonProperty("interview_style")
    @JsonAlias("interviewStyle")
    private String interviewStyle = "normal";

    @JsonProperty("retrieved_context_json")
    @JsonAlias("retrievedContextJson")
    private String retrievedContextJson;

    @JsonProperty("result_json")
    @JsonAlias("resultJson")
    private String resultJson;
}