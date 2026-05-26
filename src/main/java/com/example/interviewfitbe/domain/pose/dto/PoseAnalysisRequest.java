package com.example.interviewfitbe.domain.pose.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseAnalysisRequest {
    @JsonProperty("video_url")
    private String videoUrl;
}
