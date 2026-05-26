package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EyeBlinkingService {

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {
        String fastApiUrl = "http://127.0.0.1:8000/api/v1/vision/blinks";

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> fastApiResult = restTemplate.postForObject(
                fastApiUrl,
                request,
                Map.class
        );

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