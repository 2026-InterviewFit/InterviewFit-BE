package com.example.interviewfitbe.domain.pose.controller;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.PoseRequest;
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
        List<PoseAnalysisResultDto> results = this.poseService.getAllPostureAnalysis();
        return ResponseEntity.ok(results);
    }

    @GetMapping("{id}")
    public ResponseEntity<PoseAnalysisResultDto> getPostureAnalysis(@PathVariable Long id) {
        PoseAnalysisResultDto result = this.poseService.getPoseAnalysis(id);
        return ResponseEntity.ok(result);
    }

    // 아래 함수는 나중에 지우기
//    @PostMapping("/analyze")
//    public ResponseEntity<PoseAnalysisResultDto> analyzePosture(@RequestBody PoseRequest request) {
//        PoseAnalysisResultDto result = this.poseService.analyzeFullPosture(request.getVideoObjectKey());
//        return ResponseEntity.ok(result);
//    }
}
