package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.exception.QuestionErrorCode;
import com.example.interviewfitbe.domain.question.mapper.QuestionMapper;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionClientService questionClientService;
    private final CoverLetterService coverLetterService;
    private final JobRoleService jobRoleService;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @Transactional
    public List<QuestionDto> createQuestions(QuestionRequest request) {
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
        QuestionClientResponse response = questionClientService.createQuestions(request.getCount(), jobRole.getName(), request.getCoverLetter());
        CoverLetterEntity coverLetter = coverLetterService.getOrCreate(request.getCoverLetter());
        List<QuestionEntity> entities = response.getQuestions()
                .stream()
                .map(q -> QuestionEntity.builder()
                        .question(q)
                        .jobRole(jobRole)
                        .coverLetter(coverLetter)
                        .build())
                .toList();

        return questionRepository.saveAll(entities)
                .stream()
                .map(questionMapper::toDto)
                .toList();
    }

    public void linkQuestionToInterviewAnswer(Long questionId, InterviewAnswerEntity interviewAnswer) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND));
        question.linkInterviewAnswer(interviewAnswer);
        questionRepository.save(question);
    }
}
