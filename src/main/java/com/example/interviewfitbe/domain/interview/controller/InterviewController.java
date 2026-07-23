package com.example.interviewfitbe.domain.interview.controller;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
import com.example.interviewfitbe.domain.interview.dto.InterviewReportResponse;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartRequest;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartResponse;
import com.example.interviewfitbe.domain.interview.service.InterviewAnswerService;
import com.example.interviewfitbe.domain.interview.service.InterviewService;
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
        InterviewStartResponse response = interviewService.startInterview(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 질문별 답변 영상 업로드 API.
     */
    @PostMapping("/{interviewId}/answers")
    public ResponseEntity<InterviewAnswerUploadResponse> uploadInterviewAnswer(
            @PathVariable Long interviewId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("questionId") Long questionId,
            @RequestParam("answerOrder") Integer answerOrder,
            @RequestParam(value = "startTime", required = false) Double startTime,
            @RequestParam(value = "endTime", required = false) Double endTime
    ) throws IOException {
        InterviewAnswerUploadResponse response =
                interviewAnswerService.uploadAnswerVideo(
                        interviewId,
                        file,
                        questionId,
                        answerOrder,
                        startTime,
                        endTime
                );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{interviewId}/report")
    public ResponseEntity<InterviewReportResponse> getInterviewReport(
            @PathVariable Long interviewId
    ) {
        InterviewReportResponse response = interviewService.getInterviewReport(interviewId);
        return ResponseEntity.ok(response);
    }
}