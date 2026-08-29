package com.example.interviewfitbe.domain.question.mapper;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionResponse;
import com.example.interviewfitbe.domain.question.dto.variable.QuestionResult;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuestionMapper {
    public QuestionDto toDto(QuestionEntity entity) {
        return QuestionDto.builder()
                .id(entity.getId())
                .interviewId(entity.getInterview() == null ? null : entity.getInterview().getId())
                .question(entity.getQuestion())
                .jobName(entity.getJobRole().getName())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public QuestionResponse toResponse(QuestionEntity entity) {
        return QuestionResponse.builder()
                .id(entity.getId())
                .question(entity.getQuestion())
                .build();
    }

    public List<QuestionEntity> toEntities(QuestionResult questionResult, JobRoleEntity jobRole, List<CoverLetterQuestionAnalysisEntity> analyses) {
        Map<Integer, CoverLetterQuestionEntity> coverLetterQuestionMap =
                analyses.stream()
                        .collect(Collectors.toMap(
                                analysis -> analysis.getCoverLetterQuestion().getQuestionOrder(),
                                CoverLetterQuestionAnalysisEntity::getCoverLetterQuestion
                        ));

        return questionResult.getQuestions().stream()
                .map(item -> QuestionEntity.builder()
                        .question(item.getQuestion())
                        .type(questionResult.getType())
                        .jobRole(jobRole)
                        .coverLetterQuestion(
                                coverLetterQuestionMap.get(item.getQuestionNumber())
                        )
                        .build())
                .toList();
    }
}
