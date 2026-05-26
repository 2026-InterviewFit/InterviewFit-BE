package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.mapper.QuestionMapper;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionClientService questionClientService;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    public List<QuestionDto> createQuestions(QuestionRequest request) {
        QuestionClientResponse response = this.questionClientService.createQuestions(request.getCount(), request.getJobType(), request.getResumeContent());
        List<QuestionEntity> entities = response.getQuestions()
                .stream()
                .map(q -> QuestionEntity.builder()
                        .question(q)
                        .jobType(request.getJobType())
                        .build())
                .toList();

        return this.questionRepository.saveAll(entities)
                .stream()
                .map(this.questionMapper::toDto)
                .toList();
    }
}
