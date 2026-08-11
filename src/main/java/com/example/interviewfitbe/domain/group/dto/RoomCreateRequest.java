package com.example.interviewfitbe.domain.group.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RoomCreateRequest {

    private String title;              // 방 제목
    private Long jobRoleId;            // 직무 ID (예: 백엔드, 프론트엔드 등)
    private Integer maxParticipants;   // 최대 인원 제한 (최소 2명 ~ 최대 4명)
    private LocalDateTime scheduledStartTime; // 방장이 설정한 면접 시작 시간

    // [임시 테스트용 필드]
    // 나중에 로그인이 완성되면 RequestHeader의 JWT 토큰에서 유저 ID를 꺼내도록 변경합니다.
    private Long hostUserId;
}