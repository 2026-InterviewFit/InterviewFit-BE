package com.example.interviewfitbe.domain.job.service;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterClientResponse;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.entity.RecommendedJobEntity;
import com.example.interviewfitbe.domain.job.exception.JobRoleErrorCode;
import com.example.interviewfitbe.domain.job.exception.RecommendedJobErrorCode;
import com.example.interviewfitbe.domain.job.mapper.JobRoleMapper;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.job.repository.RecommendedJobRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RecommendJobService {
    private final RecommendedJobRepository recommendedJobRepository;
    private final JobRoleRepository jobRoleRepository;
    private final JobRoleMapper jobRoleMapper;

    public RecommendedJobEntity saveRecommendedJob(CoverLetterClientResponse analysisResult, CoverLetterEntity coverLetter) {
        JobRoleEntity jobRole = jobRoleRepository.findByName(analysisResult.getJob().getJobName())
                .or(() -> jobRoleRepository.findByName("기타"))
                .orElseThrow(() -> new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND));

        RecommendedJobEntity recommendedJob = jobRoleMapper.toEntity(jobRole, analysisResult, coverLetter);

        return recommendedJobRepository.save(recommendedJob);
    }

    public RecommendedJobEntity getRecommendedJobByCoverLetter(CoverLetterEntity coverLetter) {
        return recommendedJobRepository.findByCoverLetter(coverLetter)
                .orElseThrow(() -> new BaseException(RecommendedJobErrorCode.RECOMMENDED_JOB_NOT_FOUND));
    }
}
