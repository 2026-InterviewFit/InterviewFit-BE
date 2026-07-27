package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.dto.RoomMessageRequest;
import com.example.interviewfitbe.domain.group.dto.RoomMessageResponse;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GroupMessageController {

    /**
     * 사용자가 면접 방에 입장(Join)했을 때 호출되는 메시지 매핑
     * 프론트엔드에서 /app/chat/join/{roomId}로 메시지를 보내면,
     * 해당 방을 구독 중인(/topic/room/{roomId}) 모든 사용자에게 입장을 알림.
     */
    @MessageMapping("/room/join/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public RoomMessageResponse handleJoinRoom(@DestinationVariable Long roomId, RoomMessageRequest message) {
        return new RoomMessageResponse(
                roomId,
                message.getSender() + "님이 면접 방에 입장하셨습니다.",
                "JOIN"
        );
    }

    /**
     * AI 면접관이 다음 질문으로 넘어갈 때(순서 제어) 호출
     */
    @MessageMapping("/room/next-question/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public RoomMessageResponse handleNextQuestion(@DestinationVariable Long roomId, RoomMessageRequest message) {
        return new RoomMessageResponse(
                roomId,
                "다음 질문으로 넘어갑니다. 준비하세요!",
                "NEXT_QUESTION"
        );
    }
}