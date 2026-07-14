package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterEmbeddingService;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.*;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import com.example.interviewfitbe.domain.question.exception.QuestionErrorCode;
import com.example.interviewfitbe.domain.question.mapper.QuestionMapper;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.service.UserService;
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
    private final CoverLetterEmbeddingService coverLetterEmbeddingService;
    private final QuestionMapper questionMapper;

    private final UserService userService;

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
    public List<QuestionDto> createQuestions(Long userId, QuestionRequest request) {
        UserEntity user = userService.getUserById(userId);
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
        CoverLetterEntity coverLetter = coverLetterService.getOrCreateCoverLetter(user, request.getCoverLetter());

        List<String> previousQuestions = coverLetterEmbeddingService.findPreviousQuestions(user, coverLetter)
                .stream()
                .map(QuestionEntity::getQuestion)
                .toList();

        QuestionResult questionResult = getQuestions(request, jobRole, previousQuestions);

        List<QuestionEntity> entities = questionMapper.toEntities(questionResult, jobRole, coverLetter);

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

    private QuestionResult getQuestions(QuestionRequest request, JobRoleEntity jobRole, List<String> previousQuestions) {
        if (request.getCoverLetter() == null || request.getCoverLetter().isBlank()) {
            return QuestionResult.builder()
                    .questions(
                            jsonQuestionService.getRandomQuestions(jobRole.getName(), request.getCount()).getQuestions()
                    )
                    .type(QuestionType.DEFAULT)
                    .build();
        }
        try {
            return QuestionResult.builder()
                    .questions(
                            questionClientService.createQuestions(request.getCount(), jobRole.getName(), request.getCoverLetter(), previousQuestions).getQuestions()
                    )
                    .type(QuestionType.AI_GENERATED)
                    .build();
        } catch (Exception e) {
            return QuestionResult.builder()
                    .questions(
                            jsonQuestionService.getRandomQuestions(jobRole.getName(), request.getCount()).getQuestions()
                    )
                    .type(QuestionType.DEFAULT)
                    .build();
        }
    }
}
