package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final JobRoleService jobRoleService;
    private final FollowUpAnalysisService followUpAnalysisService;

    @Transactional
    public FollowUpCreateResponse createAndSave(FollowUpCreateRequest request) {
        validateRequest(request);

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());
        QuestionEntity question = findQuestionOrNull(request.getQuestionId());

        String jobName = resolveJobName(request, interview, question);
        String originalQuestion = resolveOriginalQuestion(request, question);
        String interviewStyle = resolveInterviewStyle(request.getInterviewStyle());

        String followUpQuestion = followUpAnalysisService.createFollowUpQuestion(
                jobName,
                originalQuestion,
                request.getAnswer(),
                interviewStyle
        );

        FollowUpQuestion saved = followUpQuestionRepository.save(
                FollowUpQuestion.builder()
                        .interview(interview)
                        .sourceQuestion(question)
                        .job(jobName)
                        .originalQuestion(originalQuestion)
                        .answer(request.getAnswer())
                        .followUpQuestion(followUpQuestion)
                        .interviewStyle(interviewStyle)
                        .build()
        );

        return new FollowUpCreateResponse(
                "꼬리질문 생성 및 저장 완료",
                saved.getId(),
                followUpQuestion
        );
    }

    private void validateRequest(FollowUpCreateRequest request) {
        if (request.getAnswer() == null || request.getAnswer().isBlank()) {
            throw new IllegalArgumentException("answer 값이 필요합니다.");
        }

        boolean hasOriginalQuestion =
                request.getOriginalQuestion() != null && !request.getOriginalQuestion().isBlank();

        boolean hasQuestionId = request.getQuestionId() != null;

        if (!hasOriginalQuestion && !hasQuestionId) {
            throw new IllegalArgumentException("originalQuestion 또는 questionId 값이 필요합니다.");
        }
    }

    private InterviewEntity findInterviewOrNull(Long interviewId) {
        if (interviewId == null) {
            return null;
        }

        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접 정보를 찾을 수 없습니다. interviewId=" + interviewId
                ));
    }

    private QuestionEntity findQuestionOrNull(Long questionId) {
        if (questionId == null) {
            return null;
        }

        return questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 질문 정보를 찾을 수 없습니다. questionId=" + questionId
                ));
    }

    private String resolveJobName(
            FollowUpCreateRequest request,
            InterviewEntity interview,
            QuestionEntity question
    ) {
        if (interview != null && interview.getJobRole() != null) {
            return interview.getJobRole().getName();
        }

        if (question != null && question.getJobRole() != null) {
            return question.getJobRole().getName();
        }

        if (request.getJobRoleId() != null) {
            JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
            return jobRole.getName();
        }

        if (request.getJob() != null && !request.getJob().isBlank()) {
            return request.getJob();
        }

        throw new IllegalArgumentException("interviewId, questionId, jobRoleId 또는 job 값이 필요합니다.");
    }

    private String resolveOriginalQuestion(
            FollowUpCreateRequest request,
            QuestionEntity question
    ) {
        if (request.getOriginalQuestion() != null && !request.getOriginalQuestion().isBlank()) {
            return request.getOriginalQuestion();
        }

        if (question != null && question.getQuestion() != null && !question.getQuestion().isBlank()) {
            return question.getQuestion();
        }

        throw new IllegalArgumentException("originalQuestion 또는 questionId 값이 필요합니다.");
    }

    private String resolveInterviewStyle(String interviewStyle) {
        if (interviewStyle == null || interviewStyle.isBlank()) {
            return "normal";
        }

        return interviewStyle;
    }
}