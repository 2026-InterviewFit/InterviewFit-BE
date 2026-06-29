package com.example.interviewfitbe.domain.question.mapper;

import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {
    public QuestionDto toDto(QuestionEntity entity) {
        return QuestionDto.builder()
                .id(entity.getId())
                .question(entity.getQuestion())
                .jobName(entity.getJobRole().getName())
                .build();
    }
}
