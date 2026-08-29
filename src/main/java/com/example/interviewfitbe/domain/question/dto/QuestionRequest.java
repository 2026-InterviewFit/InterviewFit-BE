package com.example.interviewfitbe.domain.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionRequest {
    /**
     * 생성된 질문을 연결할 1:1 면접 ID.
     *
     * 면접을 먼저 생성한 뒤 질문 생성 API를 호출하는 현재 FE 흐름에서
     * interviewId를 함께 보내면 questions.interview_id가 즉시 연결된다.
     * 기존 호출과의 호환을 위해 optional이다.
     */
    private Long interviewId;

    private Integer count;
    private Long jobRoleId;
    private String jobName;
    private Long coverLetterId;
}