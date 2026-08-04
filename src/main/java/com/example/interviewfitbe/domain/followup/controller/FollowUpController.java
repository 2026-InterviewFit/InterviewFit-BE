package com.example.interviewfitbe.domain.followup.controller;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.service.FollowUpService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/followup")
public class FollowUpController {

    private final FollowUpService followUpService;

    @PostMapping("/create")
    public FollowUpCreateResponse createFollowUpQuestion(
            @RequestBody FollowUpCreateRequest request
    ) {
        // 🔐 클라이언트가 보낸 userId는 신뢰하지 않고 JWT 사용자로 덮어씀
        Long userId = AuthUtils.getCurrentUserId();
        request.setUserId(userId);

        return followUpService.createAndSave(request);
    }
}