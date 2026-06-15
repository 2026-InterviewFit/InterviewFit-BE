package com.example.interviewfitbe.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InterviewStartResponse {

    private Long interviewId;

    private Long userId;

    private Long jobRoleId;

    private String jobName;

    private String status;
}