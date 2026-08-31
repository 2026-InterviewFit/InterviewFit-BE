package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.speech_coaching.dto.ExternalAiAnalysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAiAnalysisJsonCodec {

    private final ObjectMapper objectMapper;

    public String serialize(ExternalAiAnalysis analysis) throws JsonProcessingException {
        return analysis == null ? null : objectMapper.writeValueAsString(analysis);
    }

    public ExternalAiAnalysis deserializeSafely(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ExternalAiAnalysis.class);
        } catch (JsonProcessingException exception) {
            log.warn("external_ai_analysis JSON 파싱 실패 - 해당 필드만 null로 반환합니다.", exception);
            return null;
        }
    }
}
