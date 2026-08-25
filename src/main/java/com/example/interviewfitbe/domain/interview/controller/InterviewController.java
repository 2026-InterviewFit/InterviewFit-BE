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

    // PT 테스트용 임시 사용자 ID
    private static final Long TEST_USER_ID = 5L;

    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(
            @RequestBody InterviewStartRequest request
    ) {
        Long userId = TEST_USER_ID;

        InterviewStartResponse response =
                interviewService.startInterview(
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{interviewId}/answers")
    public ResponseEntity<InterviewAnswerUploadResponse> uploadInterviewAnswer(
            @PathVariable Long interviewId,
            @RequestPart("file") MultipartFile file,

            @RequestParam(
                    value = "questionId",
                    required = false
            )
            Long questionId,

            @RequestParam("answerOrder")
            Integer answerOrder,

            @RequestParam(
                    value = "startTime",
                    required = false
            )
            Double startTime,

            @RequestParam(
                    value = "endTime",
                    required = false
            )
            Double endTime
    ) throws IOException {

        Long userId = TEST_USER_ID;

        InterviewAnswerUploadResponse response =
                interviewAnswerService.uploadAnswerVideo(
                        userId,
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
        Long userId = TEST_USER_ID;

        InterviewReportResponse response =
                interviewService.getInterviewReport(
                        userId,
                        interviewId
                );

        return ResponseEntity.ok(response);
    }
}