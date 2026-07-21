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
    private List<CoverLetterQuestionAnalysis> questionsAnalysis;
    @JsonProperty("job_name")
    private String jobName;
    @JsonProperty("custom_job_name")
    private String customJobName;
    @JsonProperty("score")
    private Float score;
    @JsonProperty("job_reason")
    private String jobReason;
}
