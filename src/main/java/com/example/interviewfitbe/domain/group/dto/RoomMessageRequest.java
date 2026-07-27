package com.example.interviewfitbe.domain.group.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoomMessageRequest {

    private Long userId; // 로그인한 사용자 PK

    private String message; // 입장 메시지

}