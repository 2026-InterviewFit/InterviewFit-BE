package com.example.interviewfitbe.domain.pt_interview.dto;

/**
 * PT 발표 영상 시선 상세 분석
 */
public record PtEyeGazeDetailResponse(

        Double score,

        Double durationSeconds,

        Double centerRatio,

        Double gazeAwayRatio,

        Integer gazeAwayCount,

        Integer notDetectedCount,

        Double notDetectedRatio,

        String grade,

        String evaluation

) {
}