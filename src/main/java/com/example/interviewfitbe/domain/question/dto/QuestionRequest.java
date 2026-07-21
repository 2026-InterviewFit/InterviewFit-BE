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
    private Long userId; // 나중에 유저 만들면 삭제하기
    private Integer count;
    private Long jobRoleId;
    private String jobName;
    private Long coverLetterId;
}