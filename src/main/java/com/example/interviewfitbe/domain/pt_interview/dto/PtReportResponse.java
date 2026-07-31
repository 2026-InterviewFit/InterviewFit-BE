package com.example.interviewfitbe.domain.pt_interview.dto;

import java.time.LocalDateTime;

/**
 * PT 종합 리포트 조회 응답
 */
public record PtReportResponse(

        Long reportId,

        Long ptInterviewId,

        Double contentScore,

        Double structureScore,

        Double deliveryScore,

        Double qaScore,

        Double overallScore,

        String presentationSummary,

        String strengths,

        String weaknesses,

        String improvement,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}
