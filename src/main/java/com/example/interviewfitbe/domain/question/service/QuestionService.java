package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterProcessingEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.enums.ProcessingStatus;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterProcessingErrorCode;
import com.example.interviewfitbe.domain.coverletter.service.processing.CoverLetterProcessingService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionAnalysisService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionService;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.coverletter.service.embedding.CoverLetterQuestionEmbeddingService;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.*;
import com.example.interviewfitbe.domain.question.dto.variable.QuestionCreateData;
import com.example.interviewfitbe.domain.question.dto.variable.QuestionResult;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import com.example.interviewfitbe.domain.question.exception.QuestionErrorCode;
import com.example.interviewfitbe.domain.question.mapper.QuestionMapper;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.service.UserService;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CoverLetterProcessingService coverLetterProcessingService;
    private final JobRoleService jobRoleService;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    private final UserService userService;

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
        QuestionCreateData data = loadQuestionData(userId, request);
        List<QuestionEntity> entities;

        try {
            QuestionResult questionResult = getQuestions(request, data.getJobRole(), data.getPreviousQuestions(), data.getCoverLetterQuestionAnalyses());
            entities = questionMapper.toEntities(questionResult, data.getJobRole(), data.getCoverLetterQuestionAnalyses());
        } catch (Exception e) {
            QuestionResult defaultResult = getDefaultQuestions(request.getJobName(), request.getCount());
            entities = questionMapper.toEntities(defaultResult, data.getJobRole(), List.of());
        }

        return questionRepository.saveAll(entities)
                .stream()
                .map(questionMapper::toResponse)
                .toList();
    }

    private QuestionCreateData loadQuestionData(Long userId, QuestionRequest request) {
        UserEntity user = userService.getUserById(userId);
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());

        CoverLetterEntity coverLetter = null;
        List<CoverLetterQuestionAnalysisEntity> analyses = List.of();
        List<String> previousQuestions = List.of();

        if (request.getCoverLetterId() != null) {
            coverLetter = coverLetterService.getCoverLetter(request.getCoverLetterId());
            CoverLetterProcessingEntity processing = coverLetterProcessingService.getProcessingByCoverLetter(coverLetter);

            if (processing.getStatus() != ProcessingStatus.COMPLETED) {
                throw new BaseException(CoverLetterProcessingErrorCode.COVER_LETTER_PROCESSING_NOT_COMPLETED);
            }

            List<CoverLetterQuestionEntity> coverLetterQuestions = coverLetterQuestionService.getCoverLetterQuestions(coverLetter);
            analyses = coverLetterQuestionAnalysisService.getCoverLetterQuestionAnalysis(coverLetterQuestions);
            previousQuestions = coverLetterQuestionEmbeddingService.findPreviousQuestions(user, coverLetterQuestions)
                    .stream()
                    .map(QuestionEntity::getQuestion)
                    .toList();
        }

        return QuestionCreateData.builder()
                .jobRole(jobRole)
                .coverLetter(coverLetter)
                .coverLetterQuestionAnalyses(analyses)
                .previousQuestions(previousQuestions)
                .build();
    }

    private QuestionResult getQuestions(QuestionRequest request, JobRoleEntity jobRole, List<String> previousQuestions, List<CoverLetterQuestionAnalysisEntity> analyses) {
        if (request.getCoverLetterId() == null) {
            return getDefaultQuestions(request.getJobName(), request.getCount());
        }
        try {
            String jobName = "기타".equals(jobRole.getName())
                    ? request.getJobName()
                    : jobRole.getName();
            return QuestionResult.builder()
                    .questions(
                            questionClientService.createQuestions(request.getCount(), jobName, analyses, previousQuestions).getQuestions()
                    )
                    .type(QuestionType.AI_GENERATED)
                    .build();
        } catch (Exception e) {
            return getDefaultQuestions(request.getJobName(), request.getCount());
        }
    }

    private QuestionResult getDefaultQuestions(String jobName, int count) {
        return QuestionResult.builder()
                .questions(
                        jsonQuestionService.getRandomQuestions(jobName, count).getQuestions()
                )
                .type(QuestionType.DEFAULT)
                .build();
    }
}
