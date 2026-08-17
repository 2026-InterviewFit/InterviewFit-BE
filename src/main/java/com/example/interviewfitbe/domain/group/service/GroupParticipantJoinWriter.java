package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GroupParticipantJoinWriter {

    private final GroupParticipantRepository groupParticipantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GroupParticipantEntity insert(Long roomId, Long userId) {
        return groupParticipantRepository.saveAndFlush(
                GroupParticipantEntity.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .status("JOINED")
                        .build()
        );
    }

}
