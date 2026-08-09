package com.example.interviewfitbe.domain.coverletter.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRecommendation {
    @JsonProperty("job_name")
    private String jobName;

    @JsonProperty("custom_job_name")
    private String customJobName;

    @JsonProperty("score")
    private Float score;

    @JsonProperty("job_reason")
    private String jobReason;
}
