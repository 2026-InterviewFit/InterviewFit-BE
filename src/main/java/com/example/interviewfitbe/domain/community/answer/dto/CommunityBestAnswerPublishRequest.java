package com.example.interviewfitbe.domain.community.answer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommunityBestAnswerPublishRequest {

    /**
     * 공개할 본인 interview_answers.id
     */
    private Long answerId;

    /**
     * 선택 입력.
     * 비워두면 "기업 비공개"로 저장한다.
     */
    private String companyName;
}
