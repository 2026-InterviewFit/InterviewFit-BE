package com.example.interviewfitbe.domain.pose.mapper;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseNotDetectedStatistics;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class PoseMapper {
    public PoseAnalysisResultDto toDto(PoseAnalysisResultEntity entity) {
        return PoseAnalysisResultDto.builder()
                .id(entity.getId())
                .interviewId(entity.getInterview().getId())
                .interviewAnswerId(entity.getInterviewAnswer().getId())
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

    public PoseAnalysisResultEntity toEntity(Double totalDuration, PoseStatistics stats, PoseNotDetectedStatistics notDetectedStats, PoseScoreResult result, InterviewAnswerEntity interviewAnswer) {
        return PoseAnalysisResultEntity.builder()
                .interview(interviewAnswer.getInterview())
                .interviewAnswer(interviewAnswer)
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

    public PoseAnalysisResultEntity toEmptyResult(Double totalDuration, PoseNotDetectedStatistics notDetectedStats, PoseScoreResult result, InterviewAnswerEntity interviewAnswer) {
        return PoseAnalysisResultEntity.builder()
                .interview(interviewAnswer.getInterview())
                .interviewAnswer(interviewAnswer)
                .totalDuration(totalDuration)
                .badCount(0)
                .badDuration(0.0)
                .shoulderBadCount(0)
                .faceBadCount(0)
                .shoulderBadDuration(0.0)
                .faceBadDuration(0.0)
                .notDetectedDuration(notDetectedStats.getNotDetectedDuration())
                .shoulderNotDetectedDuration(notDetectedStats.getShoulderNotDetectedDuration())
                .faceNotDetectedDuration(notDetectedStats.getFaceNotDetectedDuration())
                .badRatio(0.0)
                .score(result.getScore())
                .grade(result.getGrade())
                .build();
    }
}
