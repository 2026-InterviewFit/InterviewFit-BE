package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.question.dto.QuestionResponse;
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
    private final JsonQuestionService jsonQuestionService;
    private final CoverLetterService coverLetterService;
    private final JobRoleService jobRoleService;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    public List<QuestionResponse> getAllQuestions() {
        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(questionMapper::toDto)
                .toList();
    }

    public QuestionResponse getQuestion(Long id) {
        return questionMapper.toDto(
                questionRepository.findById(id).orElseThrow(() ->
                        new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND))
        );
    }

    @Transactional
    public List<QuestionDto> createQuestions(QuestionRequest request) {
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
        QuestionClientResponse response = getQuestions(request, jobRole);
        CoverLetterEntity coverLetter = coverLetterService.getOrCreate(request.getCoverLetter());

        List<QuestionEntity> entities = response.getQuestions()
                .stream()
                .map(question -> QuestionEntity.builder()
                        .question(question)
                        .jobRole(jobRole)
                        .coverLetter(coverLetter)
                        .build())
                .toList();

        return questionRepository.saveAll(entities)
                .stream()
                .map(questionMapper::toResponse)
                .toList();
    }

    public void linkQuestionToInterview(Long questionId, InterviewEntity interview) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND));
        question.linkInterview(interview);
        questionRepository.save(question);
    }

    public void linkQuestionToInterviewAnswer(Long questionId, InterviewAnswerEntity interviewAnswer) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND));
        question.linkInterviewAnswer(interviewAnswer);
        questionRepository.save(question);
    }

    private QuestionClientResponse getQuestions(QuestionRequest request, JobRoleEntity jobRole) {
        if (request.getCoverLetter() == null || request.getCoverLetter().isBlank()) {
            return jsonQuestionService.getRandomQuestions(jobRole.getName(), request.getCount());
        }
        try {
            return questionClientService.createQuestions(request.getCount(), jobRole.getName(), request.getCoverLetter());
        } catch (Exception e) {
            return jsonQuestionService.getRandomQuestions(jobRole.getName(), request.getCount());
        }
    }
}
