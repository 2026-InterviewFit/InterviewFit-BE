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
public class PoseAnalysisResponse {
    private Integer timestamp;

    @JsonProperty("shoulder_tilt")
    private Double shoulderTilt;

    @JsonProperty("face_direction")
    private Double faceDirection;

    @JsonProperty("shoulder_status")
    private String shoulderStatus;

    @JsonProperty("face_status")
    private String faceStatus;
}
