package com.example.interviewfitbe.domain.video_storage.controller;

import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping(
            value = "/upload",
            consumes = "multipart/form-data"
    )
    public VideoUrlResponse uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("interviewId") Long interviewId
    ) throws IOException {

        log.info("========== VIDEO UPLOAD ==========");
        log.info("파일명: {}", file.getOriginalFilename());
        log.info("파일크기: {}", file.getSize());
        log.info("interviewId: {}", interviewId);
        log.info("==================================");

        return videoService.saveVideoFile(file, interviewId);
    }
}