package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.dto.RoomMessageRequest;
import com.example.interviewfitbe.domain.group.dto.RoomMessageResponse;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GroupMessageController {

    private final UserRepository userRepository;


    /**
     * 사용자가 면접 방에 입장(Join)했을 때 호출
     */
    @MessageMapping("/room/join/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public RoomMessageResponse handleJoinRoom(
            @DestinationVariable Long roomId,
            RoomMessageRequest message
    ) {

        UserEntity user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));


        return new RoomMessageResponse(
                roomId,
                user.getName() + "님이 면접 방에 입장하셨습니다.",
                "JOIN"
        );
    }


    /**
     * AI 면접관이 다음 질문으로 넘어갈 때
     */
    @MessageMapping("/room/next-question/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public RoomMessageResponse handleNextQuestion(
            @DestinationVariable Long roomId,
            RoomMessageRequest message
    ) {

        return new RoomMessageResponse(
                roomId,
                "다음 질문으로 넘어갑니다. 준비하세요!",
                "NEXT_QUESTION"
        );
    }
}