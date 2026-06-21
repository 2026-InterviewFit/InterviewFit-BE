package com.example.interviewfitbe.domain.video_storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUrlResponse {

    private String message;
    private String videoUrl;
}
