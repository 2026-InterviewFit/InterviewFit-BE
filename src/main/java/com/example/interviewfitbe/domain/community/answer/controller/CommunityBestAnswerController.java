package com.example.interviewfitbe.domain.community.answer.controller;

import com.example.interviewfitbe.domain.community.answer.dto.CommunityBestAnswerPublishRequest;
import com.example.interviewfitbe.domain.community.answer.dto.CommunityBestAnswerResponse;
import com.example.interviewfitbe.domain.community.answer.service.CommunityBestAnswerService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/answers")
public class CommunityBestAnswerController {

    private final CommunityBestAnswerService communityBestAnswerService;

    /**
     * 공개 우수 답변 목록.
     * SecurityConfig에서 GET /api/community/**는 공개되어 있다.
     */
    @GetMapping
    public List<CommunityBestAnswerResponse> getBestAnswers() {
        return communityBestAnswerService.getBestAnswers();
    }

    /**
     * 본인의 분석 완료 답변을 우수 답변으로 공개한다.
     */
    @PostMapping
    public CommunityBestAnswerResponse publish(
            @RequestBody CommunityBestAnswerPublishRequest request
    ) {
        Long userId = AuthUtils.getCurrentUserId();

        return communityBestAnswerService.publish(
                userId,
                request
        );
    }
}
