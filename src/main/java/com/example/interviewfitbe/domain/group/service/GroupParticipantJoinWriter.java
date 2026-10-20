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
        groupParticipantRepository.insertIfAbsent(roomId, userId);
        return groupParticipantRepository
                .findFirstByRoomIdAndUserIdOrderByIdAsc(roomId, userId)
                .orElseThrow(() -> new IllegalStateException("참가자 저장 결과를 찾을 수 없습니다."));
    }

}
