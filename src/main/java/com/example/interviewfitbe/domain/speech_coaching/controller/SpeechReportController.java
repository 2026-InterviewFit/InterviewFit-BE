package com.example.interviewfitbe.domain.speech_coaching.controller;

import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/speech")
public class SpeechReportController {

    private final SpeechReportService speechReportService;


    /**
     * 클라이언트 요청
     * → Spring
     * → FastAPI 분석
     * → SpeechReport + AiEvaluation 저장
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestBody SpeechAnalyzeRequest request
    ) {

        log.info(
                "스피치 분석 요청 - interviewId: {}, answerId: {}, sourceType: {}",
                request.getInterviewId(),
                request.getInterviewAnswerId(),
                request.getSourceType()
        );


        Long savedId =
                speechReportService.analyzeAndSave(request);


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "스피치 분석 및 저장 완료",
                        "reportId",
                        savedId
                )
        );
    }


    /**
     * 저장된 리포트 조회
     */
    @GetMapping("/report/{id}")
    public ResponseEntity<?> getReport(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                speechReportService.getReport(id)
        );
    }
}