package com.example.interviewfitbe.domain.analysis.service;

import com.example.interviewfitbe.domain.analysis.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.analysis.dto.VideoUrlResponse;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    public VideoUrlResponse saveVideoUrl(VideoUrlRequest request) {

        return new VideoUrlResponse(
                "Video URL received",
                request.getVideoUrl()
        );
    }
}
