package com.example.interviewfitbe.domain.livekit.controller;

import com.example.interviewfitbe.domain.livekit.dto.LiveKitTokenResponse;
import com.example.interviewfitbe.domain.livekit.service.LiveKitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/livekit")
@RequiredArgsConstructor
public class LiveKitController {

    private final LiveKitService liveKitService;


    /**
     * LiveKit 입장 토큰 발급
     */
    @PostMapping("/token")
    public ResponseEntity<LiveKitTokenResponse> createToken(
            @RequestParam String roomName,
            @RequestParam Long userId
    ) {

        String token = liveKitService.createToken(
                roomName,
                userId
        );

        return ResponseEntity.ok(
                new LiveKitTokenResponse(token)
        );
    }
}