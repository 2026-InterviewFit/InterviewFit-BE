package com.example.interviewfitbe.domain.pose.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseRequest {
    private String videoObjectKey;
}
