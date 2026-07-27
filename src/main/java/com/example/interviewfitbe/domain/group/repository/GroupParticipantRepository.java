package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupParticipantRepository extends JpaRepository<GroupParticipantEntity, Long> {
    // 특정 방에 참여한 모든 인원 조회
    List<GroupParticipantEntity> findAllByRoomId(Long roomId);
}