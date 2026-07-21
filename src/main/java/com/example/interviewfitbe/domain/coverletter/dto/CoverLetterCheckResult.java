package com.example.interviewfitbe.domain.coverletter.dto;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterCheckResult {
    private CoverLetterEntity coverLetter;
    private boolean isNew;
}
