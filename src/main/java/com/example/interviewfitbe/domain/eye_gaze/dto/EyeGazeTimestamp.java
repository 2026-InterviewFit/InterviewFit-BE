package com.example.interviewfitbe.domain.eye_gaze.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EyeGazeTimestamp {

    private Double timestamp;

    private String direction;
}
