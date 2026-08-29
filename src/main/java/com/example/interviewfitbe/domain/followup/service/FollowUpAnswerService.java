package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpAnswerCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpAnswerCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpAnswerEntity;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestionEntity;
import com.example.interviewfitbe.domain.followup.repository.FollowUpAnswerRepository;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowUpAnswerService {

    private final FollowUpAnswerRepository followUpAnswerRepository;
    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final InterviewRepository interviewRepository;

    @Transactional
    public FollowUpAnswerCreateResponse create(Long userId, FollowUpAnswerCreateRequest request) {
        validateRequest(request);

        FollowUpQuestionEntity followUpQuestion = followUpQuestionRepository
                .findById(request.getFollowUpQuestionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 꼬리질문을 찾을 수 없습니다. followUpQuestionId=" + request.getFollowUpQuestionId()
                ));

        InterviewEntity interview = resolveInterview(request, followUpQuestion);

        validateOwnershipAndRelation(userId, interview, followUpQuestion);

        FollowUpAnswerEntity answer = FollowUpAnswerEntity.builder()
                .interview(interview)
                .followUpQuestion(followUpQuestion)
                .answerText(request.getAnswerText())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .analysisStatus(AnalysisStatus.UPLOADED)
                .build();

        FollowUpAnswerEntity saved = followUpAnswerRepository.save(answer);

        return FollowUpAnswerCreateResponse.builder()
                .message("꼬리질문 답변 저장 완료")
                .followUpAnswerId(saved.getId())
                .interviewId(interview == null ? null : interview.getId())
                .followUpQuestionId(followUpQuestion.getId())
                .answerText(saved.getAnswerText())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .analysisStatus(saved.getAnalysisStatus())
                .build();
    }

    private void validateRequest(FollowUpAnswerCreateRequest request) {
        if (request.getFollowUpQuestionId() == null) {
            throw new IllegalArgumentException("followUpQuestionId 또는 follow_up_question_id 값은 필수입니다.");
        }

        if (request.getAnswerText() == null || request.getAnswerText().isBlank()) {
            throw new IllegalArgumentException("answerText는 필수입니다.");
        }
    }

    private void validateOwnershipAndRelation(
            Long userId,
            InterviewEntity interview,
            FollowUpQuestionEntity followUpQuestion
    ) {
        if (interview == null) {
            throw new IllegalArgumentException("꼬리질문 답변에 연결할 면접 정보가 없습니다.");
        }

        if (userId == null || !interview.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "본인의 면접 꼬리질문에만 답변을 저장할 수 있습니다."
            );
        }

        if (followUpQuestion.getInterview() != null
                && !followUpQuestion.getInterview().getId().equals(interview.getId())) {
            throw new IllegalArgumentException(
                    "followUpQuestionId가 현재 면접의 꼬리질문이 아닙니다."
            );
        }
    }

    private InterviewEntity resolveInterview(
            FollowUpAnswerCreateRequest request,
            FollowUpQuestionEntity followUpQuestion
    ) {
        if (request.getInterviewId() != null) {
            return interviewRepository.findById(request.getInterviewId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "해당 면접을 찾을 수 없습니다. interviewId=" + request.getInterviewId()
                    ));
        }

        return followUpQuestion.getInterview();
    }
}