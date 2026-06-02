package com.example.interviewfitbe.domain.job.mapper;

import com.example.interviewfitbe.domain.job.dto.JobRoleDto;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
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
}
