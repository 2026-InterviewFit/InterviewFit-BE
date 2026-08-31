package com.example.interviewfitbe.domain.group.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoomMessageRequest {
    private String sender; // 보낸 사람 이름 또는 ID
    private String message; // 내용
}