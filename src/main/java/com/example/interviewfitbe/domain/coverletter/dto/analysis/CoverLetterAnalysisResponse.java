package com.example.interviewfitbe.domain.coverletter.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterAnalysisResponse {
    private Long coverLetterId;
    private Long jobRoleId;
    private String recommendedJobName;
}
