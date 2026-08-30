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
     * 선택된 질문을 사용할 1:1 면접 ID.
     *
     * QuestionEntity 자체를 면접에 귀속시키지 않고,
     * interviewId가 전달되면 interview_questions 연결 테이블에
     * interview_id + question_id + question_order를 저장한다.
     *
     * 기존 질문 생성 호출과의 호환을 위해 optional이다.
     */
    private Long interviewId;

    private Integer count;
    private Long jobRoleId;
    private String jobName;
    private Long coverLetterId;
}
