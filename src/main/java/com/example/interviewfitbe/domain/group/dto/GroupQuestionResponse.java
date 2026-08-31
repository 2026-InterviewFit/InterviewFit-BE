package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupQuestionResponse {
    private Long id;
    private Integer roundNumber; // 1: 자기소개, 2: 공통, 3: 압박, 4: 직무 토의
    private String questionText; // 질문 내용

    public static GroupQuestionResponse from(GroupQuestionEntity entity) {
        return GroupQuestionResponse.builder()
                .id(entity.getId())
                .roundNumber(entity.getRoundNumber())
                .questionText(entity.getQuestionText())
                .build();
    }
}
