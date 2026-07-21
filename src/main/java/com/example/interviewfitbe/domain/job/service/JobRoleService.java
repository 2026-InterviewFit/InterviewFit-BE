package com.example.interviewfitbe.domain.job.service;

import com.example.interviewfitbe.domain.job.dto.JobRoleDto;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.exception.JobRoleErrorCode;
import com.example.interviewfitbe.domain.job.mapper.JobRoleMapper;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class JobRoleService {
    private final JobRoleRepository jobRoleRepository;
    private final JobRoleMapper jobRoleMapper;

    public List<JobRoleDto> getAllJobRoles() {
        List<JobRoleEntity> entities = jobRoleRepository.findAll();
        return entities.stream()
                .map(jobRoleMapper::toDto)
                .toList();
    }

    public List<JobRoleDto> getAllJobRolesExceptEtc() {
        List<JobRoleEntity> entities = jobRoleRepository.findAllByNameNot("기타");
        return entities.stream()
                .map(jobRoleMapper::toDto)
                .toList();
    }

    public JobRoleEntity getJobRoleEntity(Long jobId) {
        return jobRoleRepository.findById(jobId)
                .orElseThrow(() -> new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND));
    }
}
