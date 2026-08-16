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
    private Integer currentParticipantIndex;
    private InterviewStatus status;

    public static GroupInterviewSessionResponse from(GroupInterviewSessionEntity session) {
        return GroupInterviewSessionResponse.builder()
                .id(session.getId())
                .roomId(session.getInterview().getId())
                .currentRound(session.getCurrentRound())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .currentParticipantIndex(session.getCurrentParticipantIndex())
                .status(session.getStatus())
                .build();
    }
}
