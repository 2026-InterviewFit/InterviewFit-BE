package com.example.interviewfitbe.domain.pose.mapper;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import org.springframework.stereotype.Component;

@Component
public class PoseMapper {
    public PoseAnalysisResultDto toDto(PoseAnalysisResultEntity entity) {
        return PoseAnalysisResultDto.builder()
                .id(entity.getId())
                .totalDuration(entity.getTotalDuration())
                .badCount(entity.getBadCount())
                .badDuration(entity.getBadDuration())
                .shoulderBadCount(entity.getShoulderBadCount())
                .faceBadCount(entity.getFaceBadCount())
                .shoulderBadDuration(entity.getShoulderBadDuration())
                .faceBadDuration(entity.getFaceBadDuration())
                .score(entity.getScore())
                .grade(entity.getGrade())
                .build();
    }
}
