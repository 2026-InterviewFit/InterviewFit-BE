package com.example.interviewfitbe.domain.followup.controller;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.service.FollowUpService;
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
        return followUpService.createAndSave(request);
    }
}