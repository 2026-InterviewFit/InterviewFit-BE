package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.calculator.CoverLetterQuestionSelector;
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
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.dto.*;
import com.example.interviewfitbe.domain.question.dto.variable.CoverLetterQuestionData;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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
    private final InterviewRepository interviewRepository;

    private final CoverLetterQuestionSelector coverLetterQuestionSelector;

    private final UserService userService;

    public QuestionEntity getQuestion(Long id) {
        return questionRepository.findById(id).orElseThrow(() ->
                new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND));
    }

    @Transactional
    public List<QuestionResponse> createQuestions(Long userId, QuestionRequest request) {
        QuestionCreateData data = new QuestionCreateData();
        List<QuestionEntity> entities;

        try {
            data = loadQuestionData(userId, request);

            if (hasEnoughReusableQuestions(data, request)) {
                return getReusableQuestions(data, request);
            }

            entities = generateQuestions(request, data);
        } catch (BaseException e) {
            if (e.getErrorCode() == CoverLetterProcessingErrorCode.COVER_LETTER_PROCESSING_NOT_COMPLETED) {
                throw e;
            }
            else if (e.getErrorCode() == CoverLetterProcessingErrorCode.COVER_LETTER_PROCESSING_FAILED
                    && request.getCoverLetterId() != null) {
                try {
                    entities = generateQuestionsAfterProcessingFailure(request, data);
                } catch (BaseException ignored) {
                    entities = generateFallbackQuestions(request, data);
                }
            } else {
                entities = generateFallbackQuestions(request, data);
            }
        }

        entities = new ArrayList<>(entities);
        entities.add(createSelfIntroductionQuestion(data.getJobRole()));

        List<QuestionEntity> saved = saveQuestions(entities);

        return mergeQuestions(
                data.getReusableQuestions().isEmpty()
                        ? List.of()
                        : data.getReusableQuestions(),
                saved
        );
    }

    private boolean hasEnoughReusableQuestions(QuestionCreateData data, QuestionRequest request) {
        return data.getReusableQuestions() != null
                && data.getReusableQuestions().size() >= request.getCount();
    }

    private QuestionCreateData loadQuestionData(Long userId, QuestionRequest request) {
        UserEntity user = userService.getUserById(userId);
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());

        if (request.getCoverLetterId() == null) {
            return createWithoutCoverLetterData(jobRole);
        }

        return createWithCoverLetterData(user, jobRole, request);
    }

    private void validateProcessing(CoverLetterEntity coverLetter) {
        CoverLetterProcessingEntity processing = coverLetterProcessingService.getProcessingByCoverLetter(coverLetter);

        if (processing.getStatus() == ProcessingStatus.FAILED) {
            throw new BaseException(
                    CoverLetterProcessingErrorCode.COVER_LETTER_PROCESSING_FAILED
            );
        } else if (processing.getStatus() != ProcessingStatus.COMPLETED) {
            throw new BaseException(
                    CoverLetterProcessingErrorCode.COVER_LETTER_PROCESSING_NOT_COMPLETED
            );
        }
    }

    private QuestionCreateData createWithoutCoverLetterData(JobRoleEntity jobRole) {
        return QuestionCreateData.builder()
                .jobRole(jobRole)
                .reusableQuestions(List.of())
                .coverLetterQuestionAnalyses(List.of())
                .previousQuestions(List.of())
                .build();
    }

    private QuestionCreateData createWithCoverLetterData(UserEntity user, JobRoleEntity jobRole, QuestionRequest request) {
        CoverLetterEntity coverLetter = coverLetterService.getCoverLetter(request.getCoverLetterId());

        validateProcessing(coverLetter);

        CoverLetterQuestionData coverLetterData = loadCoverLetterQuestionData(coverLetter, request.getCount());

        List<QuestionEntity> previousQuestions = coverLetterData.getReusableQuestions().size() < request.getCount()
                ? loadPreviousQuestions(user, coverLetterData)
                : List.of();

        List<CoverLetterQuestionAnalysisEntity> selected = coverLetterData.getCoverLetterQuestionAnalyses().size() > (request.getCount() - coverLetterData.getReusableQuestions().size())
                ? coverLetterQuestionSelector.selectForQuestionGeneration(coverLetterData.getCoverLetterQuestionAnalyses(), previousQuestions, request.getCount() - coverLetterData.getReusableQuestions().size())
                : List.of();

        return QuestionCreateData.builder()
                .jobRole(jobRole)
                .coverLetter(coverLetter)
                .reusableQuestions(coverLetterData.getReusableQuestions())
                .previousQuestions(
                        previousQuestions
                                .stream()
                                .map(QuestionEntity::getQuestion)
                                .toList()
                )
                .coverLetterQuestionAnalyses(coverLetterData.getCoverLetterQuestionAnalyses())
                .selectedCoverLetterQuestionAnalyses(selected)
                .build();
    }

    private CoverLetterQuestionData loadCoverLetterQuestionData(CoverLetterEntity coverLetter, Integer count) {
        List<CoverLetterQuestionEntity> coverLetterQuestions = coverLetterQuestionService.getCoverLetterQuestions(coverLetter);

        List<QuestionEntity> reusableQuestions = questionRepository.findReusableQuestions(
                coverLetterQuestions.stream()
                        .map(CoverLetterQuestionEntity::getId)
                        .toList(),
                QuestionType.AI_GENERATED.name(),
                count
        );

        List<CoverLetterQuestionAnalysisEntity> analyses = coverLetterQuestionAnalysisService.getCoverLetterQuestionAnalyses(coverLetterQuestions);

        return CoverLetterQuestionData.builder()
                .reusableQuestions(reusableQuestions)
                .coverLetterQuestionAnalyses(analyses)
                .coverLetterQuestions(coverLetterQuestions)
                .build();
    }

    private List<QuestionEntity> loadPreviousQuestions(UserEntity user, CoverLetterQuestionData data) {
        return coverLetterQuestionEmbeddingService.findPreviousQuestions(
                user,
                data.getCoverLetterQuestions(),
                data.getReusableQuestions()
        );
    }

    private List<QuestionEntity> generateQuestions(QuestionRequest request, QuestionCreateData data) {
        QuestionResult result = getQuestions(
                request,
                data.getJobRole(),
                data.getPreviousQuestions() ,
                data.getSelectedCoverLetterQuestionAnalyses().isEmpty()
                        ? data.getCoverLetterQuestionAnalyses()
                        : data.getSelectedCoverLetterQuestionAnalyses(),
                data.getReusableQuestions()
        );

        return questionMapper.toEntities(result, data.getJobRole(), data.getCoverLetterQuestionAnalyses());
    }

    private List<QuestionEntity> saveQuestions(List<QuestionEntity> questions) {
        return questionRepository.saveAll(questions);
    }

    private List<QuestionResponse> getReusableQuestions(QuestionCreateData data, QuestionRequest request) {
        return data.getReusableQuestions()
                .stream()
                .limit(request.getCount())
                .map(questionMapper::toResponse)
                .toList();
    }

    private List<QuestionResponse> mergeQuestions(List<QuestionEntity> reusable, List<QuestionEntity> saved) {
        return Stream.concat(
                        reusable == null ? Stream.empty() : reusable.stream(),
                        saved.stream()
                )
                .sorted(
                        Comparator
                                .comparing(
                                        QuestionEntity::getQuestionOrder,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                )
                                .thenComparing(QuestionEntity::getId)
                )
                .map(questionMapper::toResponse)
                .toList();
    }

    private QuestionResult getQuestions(
            QuestionRequest request,
            JobRoleEntity jobRole,
            List<String> previousQuestions,
            List<CoverLetterQuestionAnalysisEntity> analyses,
            List<QuestionEntity> reusableQuestions
    ) {
        int reusableQuestionsSize = reusableQuestions != null ? reusableQuestions.size() : 0;
        int remainCount = request.getCount() - reusableQuestionsSize;

        if (request.getCoverLetterId() == null) {
            return getDefaultQuestions(request.getJobName(), remainCount);
        }

        String jobName = "기타".equals(jobRole.getName())
                ? request.getJobName()
                : jobRole.getName();

        return QuestionResult.builder()
                .questions(
                        questionClientService.createQuestions(
                                remainCount,
                                jobName,
                                analyses,
                                previousQuestions,
                                reusableQuestions != null
                                        ? reusableQuestions.stream()
                                        .map(QuestionEntity::getQuestion)
                                        .toList()
                                        : null
                        ).getQuestions()
                )
                .type(QuestionType.AI_GENERATED)
                .build();
    }

    private QuestionResult getDefaultQuestions(String jobName, int count) {
        return QuestionResult.builder()
                .questions(
                        jsonQuestionService.getRandomQuestions(jobName, count).getQuestions()
                )
                .type(QuestionType.DEFAULT)
                .build();
    }

    private List<QuestionEntity> generateQuestionsAfterProcessingFailure(QuestionRequest request, QuestionCreateData data) {
        JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());

        if (request.getCoverLetterId() == null) {
            QuestionResult result = getDefaultQuestions(jobRole.getName(), request.getCount());
            return questionMapper.toEntities(result, jobRole, List.of());
        }

        List<CoverLetterQuestionAnalysisEntity> analyses = resolveAnalyses(request.getCoverLetterId(), data).stream()
                .sorted(Comparator.comparingDouble(a -> Math.random()))
                .limit(request.getCount())
                .toList();

        QuestionResult result = QuestionResult.builder()
                .questions(
                        questionClientService.createQuestions(
                                request.getCount(),
                                request.getJobName(),
                                analyses,
                                null,
                                null
                        ).getQuestions()
                )
                .type(QuestionType.AI_GENERATED)
                .build();

        return questionMapper.toEntities(result, jobRole, analyses);
    }

    private List<CoverLetterQuestionAnalysisEntity> resolveAnalyses(Long coverLetterId, QuestionCreateData data) {
        if (data.getSelectedCoverLetterQuestionAnalyses() != null && !data.getSelectedCoverLetterQuestionAnalyses().isEmpty()) {
            return data.getSelectedCoverLetterQuestionAnalyses();
        }

        if (data.getCoverLetterQuestionAnalyses() != null && !data.getCoverLetterQuestionAnalyses().isEmpty()) {
            return data.getCoverLetterQuestionAnalyses();
        }

        CoverLetterEntity coverLetter = coverLetterService.getCoverLetter(coverLetterId);
        List<CoverLetterQuestionEntity> questions = coverLetterQuestionService.getCoverLetterQuestions(coverLetter);

        return coverLetterQuestionAnalysisService.getCoverLetterQuestionAnalyses(questions);
    }

    private List<QuestionEntity> generateFallbackQuestions(QuestionRequest request, QuestionCreateData data) {
        int reusableCount = data.getReusableQuestions() == null
                ? 0
                : data.getReusableQuestions().size();
        int generateCount = Math.max(request.getCount() - reusableCount, 0);

        String jobName = request.getJobRoleId() == 11L
                ? "기타"
                : request.getJobName();

        QuestionResult result = getDefaultQuestions(jobName, generateCount);
        return questionMapper.toEntities(result, jobRoleService.getJobRoleEntity(request.getJobRoleId()), List.of());
    }

    private QuestionEntity createSelfIntroductionQuestion(JobRoleEntity jobRole) {
        return QuestionEntity.builder()
                .question("간단하게 자기소개 부탁드립니다.")
                .jobRole(jobRole)
                .type(QuestionType.DEFAULT)
                .questionOrder(1)
                .build();
    }
}