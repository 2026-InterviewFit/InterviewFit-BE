package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpAnswerCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpAnswerCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpAnswerEntity;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import com.example.interviewfitbe.domain.followup.repository.FollowUpAnswerRepository;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowUpAnswerService {

    private final FollowUpAnswerRepository followUpAnswerRepository;
    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final InterviewRepository interviewRepository;

    @Transactional
    public FollowUpAnswerCreateResponse create(FollowUpAnswerCreateRequest request) {
        validateRequest(request);

        FollowUpQuestion followUpQuestion = followUpQuestionRepository
                .findById(request.getFollowUpQuestionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 꼬리질문을 찾을 수 없습니다. followUpQuestionId="
                                + request.getFollowUpQuestionId()
                ));

        InterviewEntity interview = resolveInterview(request, followUpQuestion);

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
                .followUpQuestionId(followUpQuestion.getId())
                .interviewId(interview == null ? null : interview.getId())
                .answerText(saved.getAnswerText())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .analysisStatus(saved.getAnalysisStatus())
                .build();
    }

    private void validateRequest(FollowUpAnswerCreateRequest request) {
        if (request.getFollowUpQuestionId() == null) {
            throw new IllegalArgumentException("followUpQuestionId는 필수입니다.");
        }

        if (request.getAnswerText() == null || request.getAnswerText().isBlank()) {
            throw new IllegalArgumentException("answerText는 필수입니다.");
        }
    }

    private InterviewEntity resolveInterview(
            FollowUpAnswerCreateRequest request,
            FollowUpQuestion followUpQuestion
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