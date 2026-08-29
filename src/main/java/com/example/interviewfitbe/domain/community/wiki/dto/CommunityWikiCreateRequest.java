package com.example.interviewfitbe.domain.community.wiki.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommunityWikiCreateRequest {

    private String companyName;

    /**
     * 대기업 / 중견기업 / 중소기업 / 스타트업
     */
    private String tier;

    /**
     * job_roles.name과 동일한 값.
     * 예: 프론트엔드, 백엔드, AI, 데이터분석
     */
    private String role;

    private String questionText;
}
