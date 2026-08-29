package com.example.interviewfitbe.domain.community.wiki.controller;

import com.example.interviewfitbe.domain.community.wiki.dto.CommunityWikiCreateRequest;
import com.example.interviewfitbe.domain.community.wiki.dto.CommunityWikiQuestionResponse;
import com.example.interviewfitbe.domain.community.wiki.service.CommunityWikiService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/wiki")
public class CommunityWikiController {

    private final CommunityWikiService communityWikiService;

    /**
     * 커뮤니티 위키 목록 조회.
     * SecurityConfig에서 GET은 공개 API로 열어둔다.
     * 로그인 상태라면 verifiedByMe도 함께 계산한다.
     */
    @GetMapping
    public List<CommunityWikiQuestionResponse> getQuestions() {
        Long currentUserId = AuthUtils.getCurrentUserIdOrNull();

        return communityWikiService.getQuestions(
                currentUserId
        );
    }

    /**
     * 새 기출문제 등록.
     * 작성자 ID는 request에서 받지 않고 JWT에서 가져온다.
     */
    @PostMapping
    public CommunityWikiQuestionResponse createQuestion(
            @RequestBody CommunityWikiCreateRequest request
    ) {
        Long userId = AuthUtils.getCurrentUserId();

        return communityWikiService.createQuestion(
                userId,
                request
        );
    }

    /**
     * 기출문제 검증 참여.
     * 같은 사용자는 같은 질문을 한 번만 검증할 수 있다.
     */
    @PostMapping("/{wikiQuestionId}/verify")
    public CommunityWikiQuestionResponse verifyQuestion(
            @PathVariable Long wikiQuestionId
    ) {
        Long userId = AuthUtils.getCurrentUserId();

        return communityWikiService.verifyQuestion(
                userId,
                wikiQuestionId
        );
    }
}
