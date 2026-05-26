package com.example.interviewfitbe.domain.question.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionClientRequest {
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("job")
    private String jobType;

    @JsonProperty("resume_content")
    private String resumeContent;
}
