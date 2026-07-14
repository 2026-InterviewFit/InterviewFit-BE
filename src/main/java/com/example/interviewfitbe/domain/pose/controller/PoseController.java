package com.example.interviewfitbe.domain.pose.controller;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/pose")
@RequiredArgsConstructor
@RestController
public class PoseController {
    private final PoseService poseService;

    @GetMapping
    public ResponseEntity<List<PoseAnalysisResultDto>> getAllPostureAnalysis() {
        List<PoseAnalysisResultDto> results = poseService.getAllPostureAnalysis();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoseAnalysisResultDto> getPostureAnalysis(@PathVariable Long id) {
        PoseAnalysisResultDto result = poseService.getPoseAnalysis(id);
        return ResponseEntity.ok(result);
    }
}
