package com.example.interviewfitbe.domain.community.answer.dto;

import com.example.interviewfitbe.domain.community.answer.entity.CommunityBestAnswerEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunityBestAnswerResponse {

    private Long id;

    /**
     * FE의 직무 필터 호환용.
     * 기존 BestAnswer.category는 세부 직무명을 사용하고 있다.
     */
    private String category;

    private String company;
    private String role;
    private int score;
    private String author;
    private String script;
    private int likeCount;
    private LocalDateTime publishedAt;

    public static CommunityBestAnswerResponse from(
            CommunityBestAnswerEntity entity
    ) {
        return CommunityBestAnswerResponse.builder()
                .id(entity.getId())
                .category(entity.getJobRole().getName())
                .company(entity.getCompanyName())
                .role(entity.getJobRole().getName())
                .score(entity.getScore())
                .author(maskName(entity.getUser().getName()))
                .script(entity.getAnswerScript())
                .likeCount(
                        entity.getLikeCount() == null
                                ? 0
                                : entity.getLikeCount()
                )
                .publishedAt(entity.getCreatedAt())
                .build();
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "익명";
        }

        String trimmed = name.trim();

        if (trimmed.length() == 1) {
            return "*";
        }

        return trimmed.substring(0, 1)
                + "*".repeat(trimmed.length() - 1);
    }
}
