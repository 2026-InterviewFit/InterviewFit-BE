package com.example.interviewfitbe.domain.pose.controller;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.PoseRequest;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/pose")
@RequiredArgsConstructor
@RestController
public class PoseController {
    private final PoseService poseService;

    @PostMapping("/analyze")
    public ResponseEntity<PoseAnalysisResultDto> analyzePosture(@RequestBody PoseRequest request) {
        PoseAnalysisResultDto result = this.poseService.analyzeFullPosture(request);
        return ResponseEntity.ok(result);
    }
}
