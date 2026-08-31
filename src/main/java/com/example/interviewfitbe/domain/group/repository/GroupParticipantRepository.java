package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupParticipantRepository extends JpaRepository<GroupParticipantEntity, Long> {

    // 특정 방에 참여한 모든 인원 조회
    List<GroupParticipantEntity> findAllByRoomId(Long roomId);

    // 특정 방의 현재 참여 인원 수 조회
    long countByRoomId(Long roomId);

    long countByRoomIdAndUserId(Long roomId, Long userId);

    // 해당 사용자가 이미 방에 참여했는지 확인
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    Optional<GroupParticipantEntity> findFirstByRoomIdAndUserIdOrderByIdAsc(Long roomId, Long userId);

    List<GroupParticipantEntity> findAllByRoomIdOrderByIdAsc(Long roomId);

    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}
