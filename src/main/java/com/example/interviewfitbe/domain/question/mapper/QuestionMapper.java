package com.example.interviewfitbe.domain.question.mapper;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionResponse;
import com.example.interviewfitbe.domain.question.dto.QuestionResult;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class QuestionMapper {
    public QuestionDto toResponse(QuestionEntity entity) {
        return QuestionDto.builder()
                .id(entity.getId())
                .question(entity.getQuestion())
                .jobName(entity.getJobRole().getName())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public QuestionResponse toDto(QuestionEntity entity) {
        return QuestionResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterview().getId())
                .interviewAnswerId(entity.getInterviewAnswer().getId())
                .question(entity.getQuestion())
                .jobName(entity.getJobRole().getName())
                .coverLetterContent(entity.getCoverLetter().getContent())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public List<QuestionEntity> toEntities(QuestionResult questionResult, JobRoleEntity jobRole, CoverLetterEntity coverLetter) {
        return questionResult.getQuestions().stream()
                .map(question -> QuestionEntity.builder()
                        .question(question)
                        .type(questionResult.getType())
                        .jobRole(jobRole)
                        .coverLetter(coverLetter)
                        .build())
                .toList();
    }
}
