package com.example.interviewfitbe.domain.pose.mapper;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import org.springframework.stereotype.Component;

@Component
public class PoseMapper {
    public PoseAnalysisResultDto toDto(PoseAnalysisResultEntity entity) {
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
                .badRatio(entity.getBadRatio())
                .score(entity.getScore())
                .grade(entity.getGrade())
                .build();
    }

    public PoseAnalysisResultEntity toEntity(Integer totalDuration, PoseStatistics stats, PoseScoreResult result, InterviewEntity interview) {
        return PoseAnalysisResultEntity.builder()
                .interview(interview)
                .totalDuration(totalDuration)
                .badCount(stats.getBadCount())
                .badDuration(stats.getBadDuration())
                .shoulderBadCount(stats.getShoulderBadCount())
                .faceBadCount(stats.getFaceBadCount())
                .shoulderBadDuration(stats.getShoulderBadDuration())
                .faceBadDuration(stats.getFaceBadDuration())
                .badRatio(result.getBadRatio())
                .score(result.getScore())
                .grade(result.getGrade())
                .build();
    }
}
