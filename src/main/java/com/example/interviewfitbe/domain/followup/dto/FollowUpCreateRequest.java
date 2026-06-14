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

    /**
     * 직무 선택 ID.
     * interviewId 또는 questionId가 없을 때 직무명을 조회하기 위해 사용한다.
     */
    private Long jobRoleId;

    /**
     * FE 단독 테스트용 직무명.
     */
    private String job;

    /**
     * 팀원 question 도메인에서 생성/저장된 질문 ID.
     * 있으면 QuestionEntity.question을 원본 질문으로 사용할 수 있다.
     */
    private Long questionId;

    /**
     * 원본 면접 질문.
     * FE와 FastAPI가 snake_case/camelCase를 섞어 쓸 수 있어 둘 다 허용한다.
     */
    @JsonProperty("original_question")
    @JsonAlias({"originalQuestion", "question"})
    private String originalQuestion;

    /**
     * 지원자의 답변 텍스트.
     * STT 결과 텍스트가 여기에 들어온다.
     */
    private String answer;

    @JsonProperty("interview_style")
    @JsonAlias("interviewStyle")
    private String interviewStyle = "normal";

    private Long userId;

    /**
     * 면접 세션 ID.
     * 있으면 FollowUpQuestion을 InterviewEntity와 FK로 연결한다.
     */
    private Long interviewId;
}