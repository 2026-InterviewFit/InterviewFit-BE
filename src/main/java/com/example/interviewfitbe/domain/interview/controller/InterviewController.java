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

    /**
     * 면접 시작 API.
     *
     * FE에서 직무 선택 후 면접 시작 버튼을 누르면 먼저 이 API를 호출한다.
     * 응답으로 받은 interviewId를 이후 질문별 영상 업로드 API에 사용한다.
     */
    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(
            @RequestBody InterviewStartRequest request
    ) {
        InterviewStartResponse response = interviewService.startInterview(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 질문별 답변 영상 업로드 API.
     *
     * FE에서 질문 하나의 답변이 끝날 때마다 호출한다.
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

    /**
     * 면접 분석 결과 조회 API.
     *
     * 대시보드에서 날짜/회차 클릭 시 이 API를 호출해서
     * eye / pose / speech 결과를 가져온다.
     */
    @GetMapping("/{interviewId}/report")
    public ResponseEntity<InterviewReportResponse> getInterviewReport(
            @PathVariable Long interviewId
    ) {
        InterviewReportResponse response = interviewService.getInterviewReport(interviewId);
        return ResponseEntity.ok(response);
    }
}