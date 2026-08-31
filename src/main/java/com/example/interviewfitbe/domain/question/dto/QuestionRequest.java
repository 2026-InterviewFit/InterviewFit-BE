package com.example.interviewfitbe.domain.question.dto;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionRequest {
    private Long interviewId;
    private Integer count;
    private Long jobRoleId;
    private String jobName;
    private Long coverLetterId;
}
