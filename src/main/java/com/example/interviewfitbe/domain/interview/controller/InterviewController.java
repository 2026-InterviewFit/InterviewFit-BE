package com.example.interviewfitbe.domain.interview.controller;

import com.example.interviewfitbe.domain.interview.dto.InterviewAnalyzeRequest;
import com.example.interviewfitbe.domain.interview.dto.InterviewAnswerUploadResponse;
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

    /**
     * 기존 통합 분석 API.
     * 지금 당장은 유지한다.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeInterview(@RequestBody InterviewAnalyzeRequest request) {
        this.interviewService.generateAnalysisResult(request);
        return ResponseEntity.ok(null);
    }

    /**
     * 질문별 답변 영상 업로드 API.
     *
     * FE에서 질문 하나의 답변이 끝날 때마다 호출한다.
     *
     * 예:
     * Q1 답변 완료 → 이 API 호출
     * Q2 답변 완료 → 이 API 호출
     * Q3 답변 완료 → 이 API 호출
     */
    @PostMapping("/{interviewId}/answers")
    public ResponseEntity<InterviewAnswerUploadResponse> uploadInterviewAnswer(
            @PathVariable Long interviewId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) Long questionId,
            @RequestParam(value = "questionText", required = false) String questionText,
            @RequestParam("answerOrder") Integer answerOrder
    ) throws IOException {
        InterviewAnswerUploadResponse response =
                interviewAnswerService.uploadAnswerVideo(
                        interviewId,
                        file,
                        questionId,
                        questionText,
                        answerOrder
                );

        return ResponseEntity.ok(response);
    }
}