package com.example.interviewfitbe.domain.analysis.controller;

import com.example.interviewfitbe.domain.analysis.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.analysis.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.analysis.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/videos")
    public VideoUrlResponse saveVideoUrl(
            @RequestBody VideoUrlRequest request
    ) {

        return analysisService.saveVideoUrl(request);
    }
}