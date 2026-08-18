package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import com.example.interviewfitbe.domain.group.entity.InterviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class InterviewProgressResponse {

    private Long roomId;
    private Integer currentRound;
    private Integer currentQuestionIndex;
    private Long currentQuestionId;
    private String currentQuestionText;
    private Integer currentParticipantIndex;
    private Long currentParticipantUserId;
    private InterviewStatus status;
    private Instant answerStartedAt;
    private Instant answerDeadlineAt;
    private Long answerId;

    public static InterviewProgressResponse from(
            Long roomId,
            GroupInterviewSessionEntity session,
            Long currentParticipantUserId,
            Long currentQuestionId,
            String currentQuestionText
    ) {
        return InterviewProgressResponse.builder()
                .roomId(roomId)
                .currentRound(session.getCurrentRound())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .currentQuestionId(currentQuestionId)
                .currentQuestionText(currentQuestionText)
                .currentParticipantIndex(session.getCurrentParticipantIndex())
                .currentParticipantUserId(currentParticipantUserId)
                .status(session.getStatus())
                .answerStartedAt(session.getAnswerStartedAt())
                .answerDeadlineAt(session.getAnswerDeadlineAt())
                .answerId(session.getCurrentAnswerId())
                .build();
    }
}
