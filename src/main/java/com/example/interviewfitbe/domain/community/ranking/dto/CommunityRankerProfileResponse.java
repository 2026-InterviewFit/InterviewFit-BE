package com.example.interviewfitbe.domain.community.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunityRankerProfileResponse {

    private Long userId;
    private int rank;
    private String name;
    private int score;
    private String tier;
    private int publishedAnswerCount;
    private int totalLikeCount;
    private List<CommunityRankerAnswerResponse> answers;
}
