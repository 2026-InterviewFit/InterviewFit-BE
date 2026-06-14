package com.example.interviewfitbe.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewAnalyzeRequest {
    private Long userId;
    private Long jobRoleId;
    private String videoObjectKey;
}
