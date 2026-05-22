package com.example.interviewfitbe.domain.VideoStorage.service;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import org.springframework.stereotype.Service;

@Service
public class VideoService {

    public VideoUrlResponse saveVideoUrl(VideoUrlRequest request) {

        return new VideoUrlResponse(
                "Video URL received",
                request.getVideoUrl()
        );
    }
}
