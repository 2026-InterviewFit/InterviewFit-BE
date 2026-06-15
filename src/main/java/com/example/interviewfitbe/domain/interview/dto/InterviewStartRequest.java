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
     * 로그인 연동 전에는 null이어도 된다.
     * null이면 임시 유저를 생성해서 면접을 시작한다.
     */
    private Long userId;

    /**
     * 선택한 직무 ID.
     *
     * 예:
     * 1 = 프론트엔드
     * 2 = 백엔드
     * 3 = AI
     */
    private Long jobRoleId;
}