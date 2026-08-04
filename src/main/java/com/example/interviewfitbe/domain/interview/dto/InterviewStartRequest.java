package com.example.interviewfitbe.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewStartRequest {

    /**
     * 선택한 직무 ID.
     * 예: 1 = 프론트엔드, 2 = 백엔드, 3 = AI
     */
    private Long jobRoleId;

    // userId는 JWT에서 추출하므로 request에서 제거
}