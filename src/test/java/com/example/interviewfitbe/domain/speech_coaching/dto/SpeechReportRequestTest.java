package com.example.interviewfitbe.domain.speech_coaching.dto;

import com.example.interviewfitbe.domain.speech_coaching.enums.SpeechScoreStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechReportRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsZeroScoreWithoutConvertingItToNull() {
        SpeechReportRequest result = convert(new HashMap<>(Map.of(
                "speech_score", 0,
                "speech_score_status", "SCORED"
        )));

        assertThat(result.getSpeechScore()).isZero();
        assertThat(result.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.SCORED);
        assertThat(result.getSpeechScoreReason()).isNull();
    }

    @Test
    void mapsMaximumScore() {
        SpeechReportRequest result = convert(Map.of(
                "speech_score", 100,
                "speech_score_status", "SCORED"
        ));

        assertThat(result.getSpeechScore()).isEqualTo(100);
        assertThat(result.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.SCORED);
    }

    @Test
    void mapsInsufficientDataAsSuccessfulNullableScore() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("speech_score", null);
        payload.put("speech_score_status", "INSUFFICIENT_DATA");
        payload.put("speech_score_reason", "발화 데이터가 부족하여 점수를 산출할 수 없습니다.");

        SpeechReportRequest result = convert(payload);

        assertThat(result.getSpeechScore()).isNull();
        assertThat(result.getSpeechScoreStatus()).isEqualTo(SpeechScoreStatus.INSUFFICIENT_DATA);
        assertThat(result.getSpeechScoreReason()).contains("발화 데이터가 부족");
    }

    private SpeechReportRequest convert(Map<String, Object> payload) {
        return objectMapper.convertValue(payload, SpeechReportRequest.class);
    }
}
