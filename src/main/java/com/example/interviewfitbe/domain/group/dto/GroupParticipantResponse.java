package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupParticipantResponse {
    private Long id;
    private Long roomId;
    private Long userId;
    private String name;
    private String status;
    private boolean host;
    private LocalDateTime joinedAt;

    public static GroupParticipantResponse from(
            GroupParticipantEntity participant,
            String name,
            Long hostUserId
    ) {
        return GroupParticipantResponse.builder()
                .id(participant.getId())
                .roomId(participant.getRoomId())
                .userId(participant.getUserId())
                .name(name)
                .status(participant.getStatus())
                .host(participant.getUserId().equals(hostUserId))
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}
