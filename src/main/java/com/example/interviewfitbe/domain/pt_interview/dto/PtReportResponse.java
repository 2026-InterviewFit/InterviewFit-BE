package com.example.interviewfitbe.domain.pt_interview.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PT 종합 리포트 조회 응답
 */
public record PtReportResponse(

        Long reportId,

        Long ptInterviewId,

        Integer contentScore,

        Integer structureScore,

        Integer deliveryScore,

        Integer qaScore,

        Integer overallScore,

        String presentationMaterialSummary,

        String actualPresentationSummary,

        String presentationComparison,

        List<PtQuestionAnalysisResponse> questionAnalyses,

        String strengths,

        String weaknesses,

        String improvement,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}