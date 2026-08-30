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
import com.example.interviewfitbe.domain.interview.entity.InterviewQuestionEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewQuestionRepository;
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
    private final InterviewQuestionRepository interviewQuestionRepository;

    private final CoverLetterQuestionSelector coverLetterQuestionSelector;

    private final UserService userService;

    public QuestionEntity getQuestion(Long id) {
        return questionRepository.findById(id).orElseThrow(() ->
                new BaseException(QuestionErrorCode.QUESTION_NOT_FOUND));
    }

    @Transactional
    public List<QuestionResponse> createQuestions(Long userId, QuestionRequest request) {
        validateCreateRequest(request);

        /*
         * FE는 면접을 먼저 생성한 뒤 질문 생성 API를 호출한다.
         * request.interviewId가 있으면 현재 로그인 사용자의 면접인지 확인하고
         * 최종 선택된 질문 원본은 건드리지 않은 채 interview_questions에 연결한다.
         */
        InterviewEntity interview = resolveInterview(userId, request);

        /*
         * 같은 interviewId로 질문 생성 API가 재호출된 경우
         * 이미 확정된 질문 세트를 그대로 반환한다.
         * 네트워크 재시도 때문에 질문 생성/Gemini 호출이 중복되는 것도 막는다.
         */
        List<QuestionResponse> existingInterviewQuestions =
                getExistingInterviewQuestions(interview, request.getCount());

        if (!existingInterviewQuestions.isEmpty()) {
            return existingInterviewQuestions;
        }

        QuestionCreateData data = new QuestionCreateData();
        List<QuestionEntity> entities = List.of();

        try {
            data = loadQuestionData(userId, request);

            if (hasEnoughReusableQuestions(data, request)) {
                List<QuestionEntity> selected = data.getReusableQuestions()
                        .stream()
                        .limit(request.getCount())
                        .toList();

                return attachToInterviewAndMap(selected, interview);
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

        List<QuestionEntity> saved = saveQuestions(entities);

        List<QuestionEntity> finalQuestions = Stream.concat(
                        data.getReusableQuestions() == null
                                ? Stream.empty()
                                : data.getReusableQuestions().stream(),
                        saved.stream()
                )
                .limit(request.getCount())
                .toList();

        return attachToInterviewAndMap(finalQuestions, interview);
    }

    private void validateCreateRequest(QuestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("질문 생성 요청이 필요합니다.");
        }

        if (request.getCount() == null || request.getCount() <= 0) {
            throw new IllegalArgumentException("count는 1 이상이어야 합니다.");
        }

        if (request.getJobRoleId() == null) {
            throw new IllegalArgumentException("jobRoleId는 필수입니다.");
        }
    }

    private InterviewEntity resolveInterview(Long userId, QuestionRequest request) {
        if (request.getInterviewId() == null) {
            return null;
        }

        InterviewEntity interview = interviewRepository
                .findById(request.getInterviewId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + request.getInterviewId()
                ));

        if (userId == null || !interview.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "본인의 면접에만 질문을 연결할 수 있습니다."
            );
        }

        if (!interview.getJobRole().getId().equals(request.getJobRoleId())) {
            throw new IllegalArgumentException(
                    "면접의 직무와 질문 생성 요청의 jobRoleId가 일치하지 않습니다."
            );
        }

        return interview;
    }

    /**
     * 질문 원본을 수정하지 않고 이번 면접과의 사용 관계만 저장한다.
     *
     * QuestionEntity는 여러 면접에서 재사용 가능한 원본이므로
     * interview_id / question_order를 QuestionEntity에 기록하지 않는다.
     * 대신 interview_questions 연결 테이블에
     * interview_id + question_id + question_order를 저장한다.
     */
    private List<QuestionResponse> attachToInterviewAndMap(
            List<QuestionEntity> questions,
            InterviewEntity interview
    ) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        if (interview != null) {
            for (int index = 0; index < questions.size(); index++) {
                QuestionEntity question = questions.get(index);
                int questionOrder = index + 1;

                InterviewQuestionEntity interviewQuestion =
                        interviewQuestionRepository
                                .findByInterview_IdAndQuestion_Id(
                                        interview.getId(),
                                        question.getId()
                                )
                                .orElseGet(
                                        () -> InterviewQuestionEntity.builder()
                                                .interview(interview)
                                                .question(question)
                                                .questionOrder(questionOrder)
                                                .build()
                                );

                if (interviewQuestion.getQuestionOrder() == null
                        || !interviewQuestion.getQuestionOrder().equals(questionOrder)) {
                    interviewQuestion.changeOrder(questionOrder);
                }

                interviewQuestionRepository.save(interviewQuestion);
            }
        }

        return questions
                .stream()
                .map(questionMapper::toResponse)
                .toList();
    }

    private List<QuestionResponse> getExistingInterviewQuestions(
            InterviewEntity interview,
            Integer expectedCount
    ) {
        if (interview == null) {
            return List.of();
        }

        List<InterviewQuestionEntity> existing =
                interviewQuestionRepository
                        .findAllByInterview_IdOrderByQuestionOrderAsc(
                                interview.getId()
                        );

        if (existing.isEmpty()) {
            return List.of();
        }

        if (expectedCount != null && existing.size() != expectedCount) {
            throw new IllegalStateException(
                    "면접 질문 연결 개수가 요청 count와 다릅니다. interviewId="
                            + interview.getId()
                            + ", linkedCount="
                            + existing.size()
                            + ", requestedCount="
                            + expectedCount
            );
        }

        return existing
                .stream()
                .map(InterviewQuestionEntity::getQuestion)
                .map(questionMapper::toResponse)
                .toList();
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
}
