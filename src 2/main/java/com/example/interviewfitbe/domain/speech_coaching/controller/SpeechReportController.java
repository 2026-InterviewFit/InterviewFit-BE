package com.example.interviewfitbe.domain.speech_coaching.controller;

import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 변경 전:
 *   POST /api/speech/report  → FastAPI가 분석 완료 후 Spring에 Push
 *
 * 변경 후:
 *   POST /api/speech/analyze → 클라이언트가 Spring에 요청
 *                               Spring이 FastAPI를 직접 호출하고 DB에 저장
 *
 * EyeBlinkingController와 동일한 패턴.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/speech")
public class SpeechReportController {

    private final SpeechReportService speechReportService;

    /**
     * 스피치 분석 트리거 엔드포인트.
     *
     * 흐름:
     * 클라이언트 → POST /api/speech/analyze
     * → SpeechReportService.analyzeAndSave()
     * → SpeechAnalysisService (WebClient로 FastAPI 호출)
     * → FastAPI 분석 (STT + Gemini)
     * → 결과 반환
     * → DB 저장
     * → 클라이언트에 reportId 응답
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody SpeechAnalyzeRequest request) {
        log.info("스피치 분석 요청 - interviewId: {}, sourceType: {}",
                request.getInterviewId(), request.getSourceType());

        Long savedId = speechReportService.analyzeAndSave(request);

        return ResponseEntity.ok(Map.of(
                "message", "스피치 분석 및 저장 완료",
                "reportId", savedId
        ));
    }

    /**
     * 저장된 리포트 단건 조회 (기존 유지)
     */
    @GetMapping("/report/{id}")
    public ResponseEntity<?> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(speechReportService.getReport(id));
    }
}
