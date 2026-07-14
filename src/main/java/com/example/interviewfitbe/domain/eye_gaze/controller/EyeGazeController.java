package com.example.interviewfitbe.domain.eye_gaze.controller;

import com.example.interviewfitbe.domain.eye_gaze.dto.EyeGazeAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_gaze.dto.EyeGazeAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_gaze.service.EyeGazeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eye-gaze")
public class EyeGazeController {

    private final EyeGazeService eyeGazeService;

    @PostMapping("/analyze")
    public EyeGazeAnalyzeResponse analyzeEyeGaze(
            @RequestBody EyeGazeAnalyzeRequest request
    ) {
        return eyeGazeService.analyzeAndSave(request);
    }

    @PostMapping(
            value = "/analyze/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EyeGazeAnalyzeResponse analyzeUploadedVideo(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "interviewId", required = false) Long interviewId
    ) {
        return eyeGazeService.analyzeUploadedVideoAndSave(
                file,
                userId,
                interviewId
        );
    }
}