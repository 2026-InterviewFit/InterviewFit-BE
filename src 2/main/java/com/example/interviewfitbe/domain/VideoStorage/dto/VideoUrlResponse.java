package com.example.interviewfitbe.domain.video_storage.dto;

public class VideoUrlResponse {

    private String message;
    private String videoUrl;

    public VideoUrlResponse(String message, String videoUrl) {
        this.message = message;
        this.videoUrl = videoUrl;
    }

    public String getMessage() {
        return message;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}
