package com.example.interviewfitbe.domain.pt_interview.dto;

/**
 * PT 발표 영상 눈 깜빡임 상세 분석
 */
public record PtEyeBlinkingDetailResponse(

        Double score,

        Double durationSeconds,

        Integer blinkCount,

        Double blinkRate,

        Integer notDetectedCount,

        Double notDetectedRatio,

        String grade,

        String feedback

) {
}
