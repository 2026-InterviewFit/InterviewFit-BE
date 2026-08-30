package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.domain.speech_coaching.dto.ExternalAiAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalAiAnalysisJsonCodecTest {

    private final ExternalAiAnalysisJsonCodec codec =
            new ExternalAiAnalysisJsonCodec(new ObjectMapper());

    @Test
    void serializesAndDeserializesStructuredAnalysis() throws Exception {
        ExternalAiAnalysis analysis = ExternalAiAnalysis.builder()
                .star(ExternalAiAnalysis.Star.builder()
                        .situation("장애 상황")
                        .task("복구 목표")
                        .action("로그 분석")
                        .result("정상화")
                        .build())
                .strengths(List.of("구체적입니다."))
                .improvements(List.of("수치가 필요합니다."))
                .build();

        ExternalAiAnalysis restored = codec.deserializeSafely(codec.serialize(analysis));

        assertThat(restored.getStar().getSituation()).isEqualTo("장애 상황");
        assertThat(restored.getStrengths()).containsExactly("구체적입니다.");
        assertThat(restored.getImprovements()).containsExactly("수치가 필요합니다.");
    }

    @Test
    void returnsNullForNullBlankOrMalformedJson() {
        assertThat(codec.deserializeSafely(null)).isNull();
        assertThat(codec.deserializeSafely(" ")).isNull();
        assertThat(codec.deserializeSafely("{broken")) .isNull();
    }
}
