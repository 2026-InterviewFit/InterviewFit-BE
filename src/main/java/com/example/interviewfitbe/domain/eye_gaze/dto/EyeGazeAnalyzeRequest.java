package com.example.interviewfitbe.domain.eye_gaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EyeGazeAnalyzeRequest {

    @JsonProperty("video_url")
    private String videoUrl;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("interview_id")
    private Long interviewId;
}