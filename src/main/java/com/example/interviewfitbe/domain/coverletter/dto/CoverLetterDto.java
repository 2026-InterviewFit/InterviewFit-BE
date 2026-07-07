package com.example.interviewfitbe.domain.coverletter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterDto {
    private Long id;
    private String content;
    private String createdAt;
}
