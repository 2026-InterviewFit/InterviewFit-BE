package com.example.interviewfitbe.domain.VideoStorage.controller;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.VideoStorage.service.VideoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
public class VideoController {

    private final VideoService analysisService;

    public VideoController(VideoService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/videos")
    public VideoUrlResponse saveVideoUrl(
            @RequestBody VideoUrlRequest request
    ) {

        return analysisService.saveVideoUrl(request);
    }
}