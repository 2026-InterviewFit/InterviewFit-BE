package com.example.interviewfitbe.domain.eye_gaze.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EyeGazeAnalyzeResponse {

    private String message;

    private Long reportId;

    private Map<String, Object> result;
}
