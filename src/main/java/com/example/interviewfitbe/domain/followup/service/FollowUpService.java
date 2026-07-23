package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestionEntity;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
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
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final QuestionRepository questionRepository;
    private final JobRoleService jobRoleService;
    private final FollowUpAnalysisService followUpAnalysisService;

    @Transactional
    public FollowUpCreateResponse createAndSave(FollowUpCreateRequest request) {
        validateRequest(request);

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());
        QuestionEntity sourceQuestion = findQuestion(request.getSourceQuestionId());
        InterviewAnswerEntity sourceAnswer = findAnswerOrNull(request.getSourceAnswerId());

        if (interview == null && sourceQuestion.getInterview() != null) {
            interview = sourceQuestion.getInterview();
        }

        if (interview == null && sourceAnswer != null) {
            interview = sourceAnswer.getInterviewSafely();
        }

        String jobName = resolveJobName(request, interview, sourceQuestion);
        String originalQuestion = resolveOriginalQuestion(request, sourceQuestion);
        String answerText = resolveAnswerText(request, sourceAnswer);
        String interviewStyle = resolveInterviewStyle(request.getInterviewStyle());

        String generatedFollowUpQuestion = followUpAnalysisService.createFollowUpQuestion(
                jobName,
                originalQuestion,
                answerText,
                interviewStyle
        );

        FollowUpQuestionEntity saved = followUpQuestionRepository.save(
                FollowUpQuestionEntity.builder()
                        .interview(interview)
                        .sourceQuestion(sourceQuestion)
                        .sourceAnswer(sourceAnswer)
                        .job(jobName)
                        .originalQuestion(originalQuestion)
                        .answer(answerText)
                        .followUpQuestion(generatedFollowUpQuestion)
                        .interviewStyle(interviewStyle)
                        .retrievedContextJson(request.getRetrievedContextJson())
                        .resultJson(request.getResultJson())
                        .build()
        );

        return FollowUpCreateResponse.builder()
                .message("꼬리질문 생성 및 저장 완료")
                .followUpQuestionId(saved.getId())
                .interviewId(interview == null ? null : interview.getId())
                .sourceQuestionId(sourceQuestion.getId())
                .sourceAnswerId(sourceAnswer == null ? null : sourceAnswer.getId())
                .job(saved.getJob())
                .originalQuestion(saved.getOriginalQuestion())
                .answer(saved.getAnswer())
                .followUpQuestion(saved.getFollowUpQuestion())
                .interviewStyle(saved.getInterviewStyle())
                .build();
    }

    private void validateRequest(FollowUpCreateRequest request) {
        if (request.getSourceQuestionId() == null) {
            throw new IllegalArgumentException("sourceQuestionId 또는 source_question_id 값은 필수입니다.");
        }

        boolean hasAnswerText =
                request.getAnswer() != null && !request.getAnswer().isBlank();

        boolean hasAnswerId =
                request.getSourceAnswerId() != null;

        if (!hasAnswerText && !hasAnswerId) {
            throw new IllegalArgumentException("answer 텍스트 또는 sourceAnswerId 값 중 하나는 필수입니다.");
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

    private QuestionEntity findQuestion(Long sourceQuestionId) {
        return questionRepository.findById(sourceQuestionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 원 질문 정보를 찾을 수 없습니다. sourceQuestionId=" + sourceQuestionId
                ));
    }

    private InterviewAnswerEntity findAnswerOrNull(Long sourceAnswerId) {
        if (sourceAnswerId == null) {
            return null;
        }

        return interviewAnswerRepository.findById(sourceAnswerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 원 답변 정보를 찾을 수 없습니다. sourceAnswerId=" + sourceAnswerId
                ));
    }

    private String resolveJobName(
            FollowUpCreateRequest request,
            InterviewEntity interview,
            QuestionEntity sourceQuestion
    ) {
        if (interview != null && interview.getJobRole() != null) {
            return interview.getJobRole().getName();
        }

        if (sourceQuestion != null && sourceQuestion.getJobRole() != null) {
            return sourceQuestion.getJobRole().getName();
        }

        if (request.getJobRoleId() != null) {
            JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
            return jobRole.getName();
        }

        if (request.getJob() != null && !request.getJob().isBlank()) {
            return request.getJob();
        }

        return "일반";
    }

    private String resolveOriginalQuestion(
            FollowUpCreateRequest request,
            QuestionEntity sourceQuestion
    ) {
        if (request.getOriginalQuestion() != null && !request.getOriginalQuestion().isBlank()) {
            return request.getOriginalQuestion();
        }

        if (sourceQuestion != null
                && sourceQuestion.getQuestion() != null
                && !sourceQuestion.getQuestion().isBlank()) {
            return sourceQuestion.getQuestion();
        }

        throw new IllegalArgumentException("originalQuestion 또는 sourceQuestionId로 원 질문을 찾을 수 없습니다.");
    }

    private String resolveAnswerText(
            FollowUpCreateRequest request,
            InterviewAnswerEntity sourceAnswer
    ) {
        if (request.getAnswer() != null && !request.getAnswer().isBlank()) {
            return request.getAnswer();
        }

        if (sourceAnswer != null
                && sourceAnswer.getAnswerText() != null
                && !sourceAnswer.getAnswerText().isBlank()) {
            return sourceAnswer.getAnswerText();
        }

        throw new IllegalArgumentException("answer 텍스트를 찾을 수 없습니다.");
    }

    private String resolveInterviewStyle(String interviewStyle) {
        if (interviewStyle == null || interviewStyle.isBlank()) {
            return "normal";
        }

        return interviewStyle;
    }
}