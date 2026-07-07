package com.example.interviewfitbe.domain.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResponse {
    private Long id;
    private Long interviewId;
    private Long interviewAnswerId;
    private String question;
    private String jobName;
    private String coverLetterContent;
    private String createdAt;
}
