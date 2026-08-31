package com.example.interviewfitbe.domain.community.ranking.controller;

import com.example.interviewfitbe.domain.community.ranking.dto.CommunityRankerProfileResponse;
import com.example.interviewfitbe.domain.community.ranking.dto.CommunityRankingResponse;
import com.example.interviewfitbe.domain.community.ranking.service.CommunityRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/rankings")
public class CommunityRankingController {

    private final CommunityRankingService communityRankingService;

    @GetMapping
    public List<CommunityRankingResponse> getRankings() {
        return communityRankingService.getTopRankings();
    }

    @GetMapping("/{userId}")
    public CommunityRankerProfileResponse getRankerProfile(
            @PathVariable Long userId
    ) {
        return communityRankingService.getRankerProfile(userId);
    }
}
