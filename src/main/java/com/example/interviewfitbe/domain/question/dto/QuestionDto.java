package com.example.interviewfitbe.domain.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDto {
    private Long id;
    private String question;
    private String jobType;
}
