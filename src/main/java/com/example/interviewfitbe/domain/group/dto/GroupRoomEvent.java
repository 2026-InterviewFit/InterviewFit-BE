package com.example.interviewfitbe.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupRoomEvent {
    private String type;
    private Long roomId;
    private Long actorUserId;
    private GroupParticipantResponse participant;
    private InterviewProgressResponse progress;
    private OffsetDateTime occurredAt;

    public static GroupRoomEvent participant(
            String type,
            Long roomId,
            Long actorUserId,
            GroupParticipantResponse participant
    ) {
        return GroupRoomEvent.builder()
                .type(type)
                .roomId(roomId)
                .actorUserId(actorUserId)
                .participant(participant)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    public static GroupRoomEvent progress(
            String type,
            Long roomId,
            Long actorUserId,
            InterviewProgressResponse progress
    ) {
        return GroupRoomEvent.builder()
                .type(type)
                .roomId(roomId)
                .actorUserId(actorUserId)
                .progress(progress)
                .occurredAt(OffsetDateTime.now())
                .build();
    }
}
