package com.example.interviewfitbe.domain.pose.controller;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.mapper.PoseMapper;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/pose")
@RequiredArgsConstructor
@RestController
public class PoseController {
    private final PoseService poseService;
    private final PoseMapper poseMapper;

    @GetMapping("/{id}")
    public ResponseEntity<PoseAnalysisResultDto> getPostureAnalysis(@PathVariable Long id) {
        PoseAnalysisResultDto result = poseMapper.toDto(poseService.getPoseAnalysis(id));
        return ResponseEntity.ok(result);
    }
}
