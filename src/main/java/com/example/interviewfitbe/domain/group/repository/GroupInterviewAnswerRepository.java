package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupInterviewAnswerRepository
        extends JpaRepository<GroupInterviewAnswerEntity, Long> {

    List<GroupInterviewAnswerEntity> findAllByGroupInterviewIdOrderByIdAsc(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from GroupInterviewAnswerEntity a " +
            "where a.id = :answerId and a.groupInterview.id = :roomId")
    Optional<GroupInterviewAnswerEntity> findByRoomIdAndIdForUpdate(
            @Param("roomId") Long roomId,
            @Param("answerId") Long answerId
    );
}
