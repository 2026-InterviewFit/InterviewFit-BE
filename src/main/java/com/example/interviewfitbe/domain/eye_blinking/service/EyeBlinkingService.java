package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EyeBlinkingService {

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {

        Map<String, Object> fastApiResult = webClient.post()
                .uri("/api/v1/vision/blinks")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (fastApiResult == null) {
            throw new RuntimeException("FastAPI 눈 깜빡임 분석 결과가 비어 있습니다.");
        }

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .userId(request.getUserId())
                .interviewId(request.getInterviewId())
                .videoUrl(request.getVideoUrl())
                .blinkCount(getInteger(fastApiResult, "blink_count"))
                .blinkRate(getDouble(fastApiResult, "blink_rate"))
                .score(getDouble(fastApiResult, "score"))
                .feedback(getString(fastApiResult, "feedback"))
                .resultJson(toJson(fastApiResult))
                .build();

        EyeBlinkingReport saved = eyeBlinkingReportRepository.save(report);

        return new EyeBlinkingAnalyzeResponse(
                "눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                fastApiResult
        );
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("눈 깜빡임 분석 결과 JSON 변환 실패", e);
        }
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).intValue();
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).doubleValue();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }
}