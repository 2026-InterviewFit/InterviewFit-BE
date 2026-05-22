package com.example.interviewfitbe.domain.VideoStorage.controller;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.VideoStorage.service.VideoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public VideoUrlResponse saveVideoUrl(
            @RequestBody VideoUrlRequest request
    ) {

        return videoService.saveVideoUrl(request);
    }
}