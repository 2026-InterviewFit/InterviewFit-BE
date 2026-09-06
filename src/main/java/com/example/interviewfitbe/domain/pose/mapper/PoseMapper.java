package com.example.interviewfitbe.domain.pose.mapper;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseLogs;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseNotDetectedStatistics;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseReportEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PoseMapper {
    public PoseAnalysisResultDto toDto(PoseReportEntity entity) {
        return PoseAnalysisResultDto.builder()
                .id(entity.getId())
                .interviewId(entity.getInterview().getId())
                .totalDuration(entity.getTotalDuration())
                .badCount(entity.getBadCount())
                .badDuration(entity.getBadDuration())
                .shoulderBadCount(entity.getShoulderBadCount())
                .faceBadCount(entity.getFaceBadCount())
                .shoulderBadDuration(entity.getShoulderBadDuration())
                .faceBadDuration(entity.getFaceBadDuration())
                .notDetectedDuration(entity.getNotDetectedDuration())
                .shoulderNotDetectedDuration(entity.getShoulderNotDetectedDuration())
                .faceNotDetectedDuration(entity.getFaceNotDetectedDuration())
                .badRatio(entity.getBadRatio())
                .score(entity.getScore())
                .grade(entity.getGrade().name())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public PoseReportEntity toEntity(Double totalDuration, PoseStatistics stats, PoseNotDetectedStatistics notDetectedStats, PoseScoreResult result, InterviewEntity interview) {
        return PoseReportEntity.builder()
                .interview(interview)
                .totalDuration(totalDuration)
                .badCount(stats.getBadCount())
                .badDuration(stats.getBadDuration())
                .shoulderBadCount(stats.getShoulderBadCount())
                .faceBadCount(stats.getFaceBadCount())
                .shoulderBadDuration(stats.getShoulderBadDuration())
                .faceBadDuration(stats.getFaceBadDuration())
                .notDetectedDuration(notDetectedStats.getNotDetectedDuration())
                .shoulderNotDetectedDuration(notDetectedStats.getShoulderNotDetectedDuration())
                .faceNotDetectedDuration(notDetectedStats.getFaceNotDetectedDuration())
                .badRatio(result.getBadRatio())
                .score(result.getScore())
                .grade(result.getGrade())
                .build();
    }

    public PoseReportEntity toEmptyResult(InterviewEntity interview) {
        return PoseReportEntity.builder()
                .interview(interview)
                .totalDuration(0.0)
                .badCount(0)
                .badDuration(0.0)
                .shoulderBadCount(0)
                .faceBadCount(0)
                .shoulderBadDuration(0.0)
                .faceBadDuration(0.0)
                .notDetectedDuration(0.0)
                .shoulderNotDetectedDuration(0.0)
                .faceNotDetectedDuration(0.0)
                .badRatio(0.0)
                .score(0)
                .grade(PoseGrade.POOR)
                .build();
    }

    public PoseBadLogEntity toBadLog(Double timestamp, PoseIssueType issueType, Double deviation) {
        return PoseBadLogEntity.builder()
                .timestamp(timestamp)
                .issueType(issueType)
                .deviation(deviation)
                .build();
    }

    public PoseNotDetectedLogEntity toNotDetectedLog(Double timestamp, PoseIssueType issueType) {
        return PoseNotDetectedLogEntity.builder()
                .timestamp(timestamp)
                .issueType(issueType)
                .build();
    }

    public PoseLogs toPoseLogs(List<PoseBadLogEntity> badLogs, List<PoseNotDetectedLogEntity> notDetectedLogs) {
        return PoseLogs.builder()
                .badLogs(badLogs)
                .notDetectedLogs(notDetectedLogs)
                .build();
    }

    public PoseAnalysisResultDto toEmptyResult() {
        return PoseAnalysisResultDto.builder()
                .totalDuration(0.0)
                .badCount(0)
                .badDuration(0.0)
                .shoulderBadCount(0)
                .faceBadCount(0)
                .shoulderBadDuration(0.0)
                .faceBadDuration(0.0)
                .notDetectedDuration(0.0)
                .shoulderNotDetectedDuration(0.0)
                .faceNotDetectedDuration(0.0)
                .badRatio(0.0)
                .score(0)
                .grade(PoseGrade.POOR.name())
                .build();
    }

    public PoseAnalysisResultDto toAnalysisResult(Double totalDuration, PoseStatistics stats, PoseNotDetectedStatistics notDetectedStats, PoseScoreResult result) {
        return PoseAnalysisResultDto.builder()
                .totalDuration(totalDuration)
                .badCount(stats.getBadCount())
                .badDuration(stats.getBadDuration())
                .shoulderBadCount(stats.getShoulderBadCount())
                .faceBadCount(stats.getFaceBadCount())
                .shoulderBadDuration(stats.getShoulderBadDuration())
                .faceBadDuration(stats.getFaceBadDuration())
                .notDetectedDuration(notDetectedStats.getNotDetectedDuration())
                .shoulderNotDetectedDuration(notDetectedStats.getShoulderNotDetectedDuration())
                .faceNotDetectedDuration(notDetectedStats.getFaceNotDetectedDuration())
                .badRatio(result.getBadRatio())
                .score(result.getScore())
                .grade(result.getGrade().name())
                .build();
    }
}
