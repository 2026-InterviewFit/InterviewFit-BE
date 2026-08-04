package com.example.interviewfitbe.domain.interview.controller;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.dto.InterviewReportResponse;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartRequest;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartResponse;
import com.example.interviewfitbe.domain.interview.service.InterviewAnswerService;
import com.example.interviewfitbe.domain.interview.service.InterviewService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequestMapping("/api/interview")
@RequiredArgsConstructor
@RestController
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewAnswerService interviewAnswerService;

    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(
            @RequestBody InterviewStartRequest request
    ) {
        Long userId = AuthUtils.getCurrentUserId();
        InterviewStartResponse response = interviewService.startInterview(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{interviewId}/answers")
    public ResponseEntity<InterviewAnswerUploadResponse> uploadInterviewAnswer(
            @PathVariable Long interviewId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("questionId") Long questionId,
            @RequestParam("answerOrder") Integer answerOrder,
            @RequestParam(value = "startTime", required = false) Double startTime,
            @RequestParam(value = "endTime", required = false) Double endTime
    ) throws IOException {
        Long userId = AuthUtils.getCurrentUserId();
        InterviewAnswerUploadResponse response =
                interviewAnswerService.uploadAnswerVideo(
                        userId, interviewId, file, questionId, answerOrder, startTime, endTime
                );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{interviewId}/report")
    public ResponseEntity<InterviewReportResponse> getInterviewReport(
            @PathVariable Long interviewId
    ) {
        Long userId = AuthUtils.getCurrentUserId();
        InterviewReportResponse response = interviewService.getInterviewReport(userId, interviewId);
        return ResponseEntity.ok(response);
    }
}