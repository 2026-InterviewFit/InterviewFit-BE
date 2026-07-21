package com.example.interviewfitbe.domain.coverletter.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterAnalysisRequest {
    private Long userId; // 나중에 유저 만들면 삭제하기
    private String coverLetter;

    public void updateCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
}
