package com.example.interviewfitbe.domain.coverletter.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterClientResponse {
    @JsonProperty("questions_analysis")
    private List<CoverLetterQuestionAnalysisItem> questionsAnalysis;

    @JsonProperty("job")
    private JobRecommendation job;
}
