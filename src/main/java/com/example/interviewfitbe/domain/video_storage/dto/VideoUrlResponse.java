package com.example.interviewfitbe.domain.video_storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoUrlResponse {

    private String message;
    private String videoUrl;
}