package com.example.interviewfitbe.domain.speech_coaching.controller;

import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechReportRequest;
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
     * FastAPI(speech_service.py)에서 분석 완료된 리포트를 수신
     * URL: POST http://localhost:8080/api/speech/report
     */
    @PostMapping("/report")
    public ResponseEntity<?> receiveReport(@RequestBody SpeechReportRequest request) {
        // 수정: jobCategory 필드가 삭제되었으므로 request에서는 불러올 수 없음.
        // 대신 interviewId를 사용하여 어떤 면접의 결과인지 로그에 남김
        log.info("FastAPI로부터 스피치 리포트 수신 - 면접 ID: {}", request.getInterviewId());
        log.debug("수신 데이터: {}", request);

        Long savedId = speechReportService.saveReport(request);

        return ResponseEntity.ok(Map.of(
                "message", "리포트 저장 완료",
                "reportId", savedId
        ));
    }

    /**
     * 저장된 리포트 단건 조회
     */
    @GetMapping("/report/{id}")
    public ResponseEntity<?> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(speechReportService.getReport(id));
    }
}