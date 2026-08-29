package com.example.interviewfitbe.domain.community.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunityRankingResponse {

    private Long userId;
    private int rank;
    private String name;
    private int score;
    private String tier;
    private String metricType;
    private String metricValue;
    private String script;
    private int publishedAnswerCount;
    private int totalLikeCount;
    private String role;
}
