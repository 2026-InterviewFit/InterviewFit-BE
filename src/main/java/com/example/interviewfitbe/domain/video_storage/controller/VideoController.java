package com.example.interviewfitbe.domain.video_storage.controller;

import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/upload")
    public VideoUrlResponse uploadVideo(
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return videoService.saveVideoFile(file);
    }
}