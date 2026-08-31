package com.example.interviewfitbe.domain.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoomMessageResponse {
    private Long roomId;
    private String message;
    private String type; // "JOIN", "LEAVE", "NEXT_QUESTION" 등 상태 구분 값
}