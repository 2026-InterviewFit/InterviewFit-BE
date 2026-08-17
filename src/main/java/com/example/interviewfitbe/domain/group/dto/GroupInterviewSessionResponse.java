package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import com.example.interviewfitbe.domain.group.entity.InterviewStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupInterviewSessionResponse {

    private Long id;
    private Long roomId;
    private Integer currentRound;
    private Integer currentQuestionIndex;
    private Long currentQuestionId;
    private String currentQuestionText;
    private Integer currentParticipantIndex;
    private Long currentParticipantUserId;
    private InterviewStatus status;

    public static GroupInterviewSessionResponse from(
            GroupInterviewSessionEntity session,
            Long currentParticipantUserId,
            Long currentQuestionId,
            String currentQuestionText
    ) {
        return GroupInterviewSessionResponse.builder()
                .id(session.getId())
                .roomId(session.getInterview().getId())
                .currentRound(session.getCurrentRound())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .currentQuestionId(currentQuestionId)
                .currentQuestionText(currentQuestionText)
                .currentParticipantIndex(session.getCurrentParticipantIndex())
                .currentParticipantUserId(currentParticipantUserId)
                .status(session.getStatus())
                .build();
    }
}
