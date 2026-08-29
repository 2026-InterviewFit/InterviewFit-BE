package com.example.interviewfitbe.domain.community.wiki.dto;

import com.example.interviewfitbe.domain.community.wiki.entity.CommunityWikiQuestionEntity;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommunityWikiQuestionResponse {

    private Long id;
    private String companyName;
    private String tier;
    private String role;
    private String text;
    private String date;
    private int verifiedCount;
    private boolean verified;
    private boolean verifiedByMe;

    public static CommunityWikiQuestionResponse from(
            CommunityWikiQuestionEntity entity,
            boolean verifiedByMe
    ) {
        String formattedDate = entity.getCreatedAt() == null
                ? null
                : entity.getCreatedAt().format(
                        DateTimeFormatter.ofPattern("yyyy.MM.dd")
                );

        return CommunityWikiQuestionResponse.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .tier(entity.getCompanyTier())
                .role(entity.getJobRole().getName())
                .text(entity.getQuestionText())
                .date(formattedDate)
                .verifiedCount(entity.getVerifiedCount())
                .verified(
                        entity.getVerifiedCount()
                                >= CommunityWikiQuestionEntity.REQUIRED_VERIFICATION_COUNT
                )
                .verifiedByMe(verifiedByMe)
                .build();
    }
}
