package com.example.interviewfitbe.domain.video_storage.controller;

import com.example.interviewfitbe.domain.video_storage.service.MinioVideoStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoStreamController {
    private final MinioVideoStreamingService streamingService;

    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            @RequestParam String objectKey,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        return streamingService.stream(objectKey, rangeHeader);
    }
}
