package com.example.interviewfitbe.domain.community.ranking.dto;

import com.example.interviewfitbe.domain.community.answer.entity.CommunityBestAnswerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunityRankerAnswerResponse {

    private Long id;
    private String company;
    private String role;
    private int score;
    private String script;
    private int likeCount;
    private LocalDateTime publishedAt;

    public static CommunityRankerAnswerResponse from(
            CommunityBestAnswerEntity entity
    ) {
        return CommunityRankerAnswerResponse.builder()
                .id(entity.getId())
                .company(entity.getCompanyName())
                .role(entity.getJobRole().getName())
                .score(entity.getScore())
                .script(entity.getAnswerScript())
                .likeCount(
                        entity.getLikeCount() == null
                                ? 0
                                : entity.getLikeCount()
                )
                .publishedAt(entity.getCreatedAt())
                .build();
    }
}
