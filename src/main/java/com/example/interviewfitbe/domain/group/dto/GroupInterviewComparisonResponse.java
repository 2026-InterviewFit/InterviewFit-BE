package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class GroupInterviewComparisonResponse {
    private Long reportId;
    private Long roomId;
    private Long participantUserId;
    private GroupComparisonScope scope;
    private Integer questionIndex;
    private BigDecimal contentScore;
    private BigDecimal speechScore;
    private BigDecimal gazeScore;
    private BigDecimal poseScore;
    private BigDecimal blinkScore;
    private BigDecimal totalScore;
    private Integer rank;
    private Integer participantCount;
    private String strengthSummary;
    private String improvementSummary;
    private GroupComparisonStatus status;

    public static GroupInterviewComparisonResponse from(GroupInterviewComparisonReportEntity report) {
        return GroupInterviewComparisonResponse.builder()
                .reportId(report.getId()).roomId(report.getRoomId())
                .participantUserId(report.getParticipantUserId()).scope(report.getScope())
                .questionIndex(report.getQuestionIndex()).contentScore(report.getContentScore())
                .speechScore(report.getSpeechScore()).gazeScore(report.getGazeScore())
                .poseScore(report.getPoseScore()).blinkScore(report.getBlinkScore())
                .totalScore(report.getTotalScore()).rank(report.getRank())
                .participantCount(report.getParticipantCount())
                .strengthSummary(report.getStrengthSummary())
                .improvementSummary(report.getImprovementSummary()).status(report.getStatus())
                .build();
    }
}
