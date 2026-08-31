package com.example.interviewfitbe.domain.job.mapper;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterClientResponse;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.job.dto.JobRoleDto;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.entity.RecommendedJobEntity;
import org.springframework.stereotype.Component;

@Component
public class JobRoleMapper {
    public JobRoleDto toDto(JobRoleEntity entity) {
        return JobRoleDto.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .name(entity.getName())
                .build();
    }

    public RecommendedJobEntity toEntity(JobRoleEntity jobRole, CoverLetterClientResponse analysisResult, CoverLetterEntity coverLetter) {
        return RecommendedJobEntity.builder()
                .coverLetter(coverLetter)
                .jobRole(jobRole)
                .customJobName(analysisResult.getJob().getCustomJobName())
                .score(analysisResult.getJob().getScore())
                .reason(analysisResult.getJob().getJobReason())
                .build();
    }
}
