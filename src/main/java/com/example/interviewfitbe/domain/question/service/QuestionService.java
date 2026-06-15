package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.mapper.QuestionMapper;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionClientService questionClientService;
    private final QuestionRepository questionRepository;
    private final CoverLetterService coverLetterService;
    private final JobRoleService jobRoleService;
    private final QuestionMapper questionMapper;

    @Transactional
    public List<QuestionDto> createQuestions(QuestionRequest request) {
        JobRoleEntity jobRole = this.jobRoleService.getJobRoleEntity(request.getJobRoleId());

        QuestionClientResponse response = this.questionClientService.createQuestions(request.getCount(), jobRole.getName(), request.getCoverLetter());

        CoverLetterEntity coverLetter = this.coverLetterService.getOrCreate(request.getCoverLetter());

        List<QuestionEntity> entities = response.getQuestions()
                .stream()
                .map(q -> QuestionEntity.builder()
                        .question(q)
                        .jobRole(jobRole)
                        .coverLetter(coverLetter)
                        .build())
                .toList();

        return this.questionRepository.saveAll(entities)
                .stream()
                .map(this.questionMapper::toDto)
                .toList();
    }
}
