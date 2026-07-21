package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionAnalysisService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionService;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.coverletter.service.embedding.CoverLetterQuestionEmbeddingService;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.*;
import com.example.interviewfitbe.domain.question.dto.QuestionResult;
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
    private final CoverLetterQuestionAnalysisService coverLetterQuestionAnalysisService;
    private final CoverLetterQuestionService coverLetterQuestionService;
    private final CoverLetterQuestionEmbeddingService coverLetterQuestionEmbeddingService;
    private final JobRoleService jobRoleService;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    private final UserService userService;

    @Transactional
    public void assignInterview(Long questionId, InterviewEntity interview) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND));

        question.assignInterview(interview);
    }

    @Transactional
    public void assignInterviewToQuestions(List<Long> questionIds, InterviewEntity interview) {
        List<QuestionEntity> questions = questionRepository.findAllById(questionIds);

        questions.forEach(question -> question.assignInterview(interview));
    }

    public List<QuestionDto> getAllQuestions() {
        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(questionMapper::toDto)
                .toList();
    }

    public QuestionDto getQuestion(Long id) {
        return questionMapper.toDto(
                questionRepository.findById(id).orElseThrow(() ->
                        new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND))
        );
    }

    @Transactional
    public List<QuestionResponse> createQuestions(Long userId, QuestionRequest request) {
        UserEntity user = userService.getUserById(userId);
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());

        CoverLetterEntity coverLetter = null;
        List<CoverLetterQuestionAnalysisEntity> coverLetterQuestionAnalysis = List.of();
        List<String> previousQuestions = List.of();

        if (request.getCoverLetterId() != null) {
            coverLetter = coverLetterService.getCoverLetter(request.getCoverLetterId());
            List<CoverLetterQuestionEntity> coverLetterQuestions = coverLetterQuestionService.getCoverLetterQuestions(coverLetter);
            coverLetterQuestionAnalysis = coverLetterQuestionAnalysisService.getCoverLetterQuestionAnalysis(coverLetterQuestions);
            previousQuestions = coverLetterQuestionEmbeddingService.findPreviousQuestions(user, coverLetterQuestions)
                    .stream()
                    .map(QuestionEntity::getQuestion)
                    .toList();
        }

        QuestionResult questionResult = getQuestions(request, jobRole, previousQuestions, coverLetterQuestionAnalysis);

        List<QuestionEntity> entities = questionMapper.toEntities(questionResult, jobRole, coverLetter);

        return questionRepository.saveAll(entities)
                .stream()
                .map(questionMapper::toResponse)
                .toList();
    }

    private QuestionResult getQuestions(QuestionRequest request, JobRoleEntity jobRole, List<String> previousQuestions, List<CoverLetterQuestionAnalysisEntity> coverLetterQuestionAnalysis) {
        if (coverLetterQuestionAnalysis == null || coverLetterQuestionAnalysis.isEmpty()) {
            return QuestionResult.builder()
                    .questions(
                            jsonQuestionService.getRandomQuestions(jobRole.getName(), request.getCount()).getQuestions()
                    )
                    .type(QuestionType.DEFAULT)
                    .build();
        }
        try {
            String jobName = jobRole.getName();
            if ("기타".equals(jobName)) {
                jobName = request.getJobName();
            }
            return QuestionResult.builder()
                    .questions(
                            questionClientService.createQuestions(request.getCount(), jobName, coverLetterQuestionAnalysis, previousQuestions).getQuestions()
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
