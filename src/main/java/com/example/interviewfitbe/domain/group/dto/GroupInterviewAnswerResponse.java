package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GroupInterviewAnswerResponse {
    private Long answerId;
    private Long roomId;
    private Long participantUserId;
    private Integer round;
    private Integer questionIndex;
    private Long questionId;
    private Instant answerStartedAt;
    private Instant answerCompletedAt;
    private GroupAnswerCompletionType completionType;
    private boolean videoUploaded;
    private String videoObjectKey;

    public static GroupInterviewAnswerResponse from(GroupInterviewAnswerEntity answer) {
        return GroupInterviewAnswerResponse.builder()
                .answerId(answer.getId())
                .roomId(answer.getGroupInterview().getId())
                .participantUserId(answer.getParticipantUserId())
                .round(answer.getRound())
                .questionIndex(answer.getQuestionIndex())
                .questionId(answer.getQuestionId())
                .answerStartedAt(answer.getAnswerStartedAt())
                .answerCompletedAt(answer.getAnswerCompletedAt())
                .completionType(answer.getCompletionType())
                .videoUploaded(answer.isVideoUploaded())
                .videoObjectKey(answer.getVideoObjectKey())
                .build();
    }
}
