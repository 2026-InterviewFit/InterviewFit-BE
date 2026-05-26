package com.example.interviewfitbe.domain.eye_blinking.controller;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}