package com.example.interviewfitbe.domain.eye_blinking.controller;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eye-blinking")
public class EyeBlinkingController {

    private final EyeBlinkingService eyeBlinkingService;

    @PostMapping("/analyze")
    public EyeBlinkingAnalyzeResponse analyzeEyeBlinking(
            @RequestBody EyeBlinkingAnalyzeRequest request
    ) {
        return eyeBlinkingService.analyzeAndSave(request);
    }

    @PostMapping(
            value = "/analyze/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EyeBlinkingAnalyzeResponse analyzeUploadedVideo(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "interviewId", required = false) Long interviewId
    ) {
        return eyeBlinkingService.analyzeUploadedVideoAndSave(file, userId, interviewId);
    }
}