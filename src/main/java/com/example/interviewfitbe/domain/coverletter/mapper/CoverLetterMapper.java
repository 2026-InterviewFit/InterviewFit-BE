package com.example.interviewfitbe.domain.coverletter.mapper;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterCheckResult;
import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterDto;
import com.example.interviewfitbe.domain.coverletter.dto.analysis.CoverLetterAnalysisResponse;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.job.entity.RecommendedJobEntity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class CoverLetterMapper {
    public CoverLetterDto toDto(CoverLetterEntity entity) {
        return CoverLetterDto.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public CoverLetterAnalysisResponse toResponse(Long coverLetterId, RecommendedJobEntity recommendedJob) {
        String recommendedJobName = recommendedJob.getJobRole().getName();

        if ("기타".equals(recommendedJobName)) {
            recommendedJobName = recommendedJob.getCustomJobName();
        }

        return CoverLetterAnalysisResponse.builder()
                .coverLetterId(coverLetterId)
                .jobRoleId(recommendedJob.getJobRole().getId())
                .recommendedJobName(recommendedJobName)
                .build();
    }

    public CoverLetterCheckResult toCheck(CoverLetterEntity entity, boolean isNew) {
        return CoverLetterCheckResult.builder()
                .coverLetter(entity)
                .isNew(isNew)
                .build();
    }
}
