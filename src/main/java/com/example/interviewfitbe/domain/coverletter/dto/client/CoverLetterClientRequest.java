package com.example.interviewfitbe.domain.coverletter.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterClientRequest {
    private String coverletter;
}
