package com.example.interviewfitbe.domain.interview.controller;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnalyzeRequest;
import com.example.interviewfitbe.domain.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/interview")
@RequiredArgsConstructor
@RestController
public class InterviewController {
    private final InterviewService interviewService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeInterview(@RequestBody InterviewAnalyzeRequest request) {
        this.interviewService.generateAnalysisResult(request);
        return ResponseEntity.ok(null);
    }
}
