package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
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
        QuestionEntity sourceQuestion = findQuestion(request.getQuestionId());
        InterviewAnswerEntity sourceAnswer = findAnswerOrNull(request.getSourceAnswerId());

        String jobName = resolveJobName(request, interview, sourceQuestion);
        String originalQuestion = resolveOriginalQuestion(request, sourceQuestion);
        String answerText = resolveAnswerText(request, sourceAnswer);
        String interviewStyle = resolveInterviewStyle(request.getInterviewStyle());

        String followUpQuestion = followUpAnalysisService.createFollowUpQuestion(
                jobName,
                originalQuestion,
                answerText,
                interviewStyle
        );

        FollowUpQuestion saved = followUpQuestionRepository.save(
                FollowUpQuestion.builder()
                        .interview(interview)
                        .sourceQuestion(sourceQuestion)
                        .sourceAnswer(sourceAnswer)
                        .job(jobName)
                        .originalQuestion(originalQuestion)
                        .answer(answerText)
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
        if (request.getQuestionId() == null) {
            throw new IllegalArgumentException("questionId는 필수입니다.");
        }

        boolean hasAnswerText =
                request.getAnswer() != null && !request.getAnswer().isBlank();
        boolean hasAnswerId = request.getSourceAnswerId() != null;

        if (!hasAnswerText && !hasAnswerId) {
            throw new IllegalArgumentException(
                    "answer 텍스트 또는 sourceAnswerId 중 하나는 필수입니다."
            );
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

    private QuestionEntity findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 질문 정보를 찾을 수 없습니다. questionId=" + questionId
                ));
    }

    private InterviewAnswerEntity findAnswerOrNull(Long answerId) {
        if (answerId == null) {
            return null;
        }

        return interviewAnswerRepository.findById(answerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 답변 정보를 찾을 수 없습니다. sourceAnswerId=" + answerId
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

        throw new IllegalArgumentException(
                "interviewId, questionId, jobRoleId 또는 job 값이 필요합니다."
        );
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

        throw new IllegalArgumentException("원본 질문을 찾을 수 없습니다.");
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

        throw new IllegalArgumentException("answer 텍스트가 필요합니다.");
    }

    private String resolveInterviewStyle(String interviewStyle) {
        if (interviewStyle == null || interviewStyle.isBlank()) {
            return "normal";
        }

        return interviewStyle;
    }
}