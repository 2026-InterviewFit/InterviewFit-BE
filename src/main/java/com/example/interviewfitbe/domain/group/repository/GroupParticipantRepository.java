package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupParticipantRepository extends JpaRepository<GroupParticipantEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO group_participants (room_id, user_id, status, joined_at)
            VALUES (:roomId, :userId, 'JOINED', CURRENT_TIMESTAMP)
            ON CONFLICT (room_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("roomId") Long roomId, @Param("userId") Long userId);

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
