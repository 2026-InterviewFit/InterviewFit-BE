package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import com.example.interviewfitbe.domain.group.entity.InterviewStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewProgressResponse {

    private Long roomId;
    private Integer currentRound;
    private Integer currentQuestionIndex;
    private Integer currentParticipantIndex;
    private InterviewStatus status;

    public static InterviewProgressResponse from(
            Long roomId,
            GroupInterviewSessionEntity session
    ) {
        return InterviewProgressResponse.builder()
                .roomId(roomId)
                .currentRound(session.getCurrentRound())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .currentParticipantIndex(session.getCurrentParticipantIndex())
                .status(session.getStatus())
                .build();
    }
}
