package com.example.interviewfitbe.domain.group.controller;

import org.springframework.stereotype.Controller;

@Controller
public class GroupMessageController {
    // 방 상태 변경은 권한 검증과 DB 트랜잭션을 위해 REST API로만 받고,
    // 결과는 /topic/room/{roomId}로 broadcast한다.
}
