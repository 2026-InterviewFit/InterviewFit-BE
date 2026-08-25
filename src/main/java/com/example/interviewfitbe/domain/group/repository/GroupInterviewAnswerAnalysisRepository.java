package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

public interface GroupInterviewAnswerAnalysisRepository
        extends JpaRepository<GroupInterviewAnswerAnalysisEntity, Long> {

    @EntityGraph(attributePaths = {"answer", "answer.groupInterview"})
    Optional<GroupInterviewAnswerAnalysisEntity> findByAnswerId(Long answerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"answer", "answer.groupInterview"})
    @Query("select a from GroupInterviewAnswerAnalysisEntity a where a.answer.id = :answerId")
    Optional<GroupInterviewAnswerAnalysisEntity> findByAnswerIdForUpdate(
            @Param("answerId") Long answerId
    );

    @EntityGraph(attributePaths = {"answer", "answer.groupInterview"})
    @Query("select a from GroupInterviewAnswerAnalysisEntity a " +
            "where a.answer.groupInterview.id = :roomId " +
            "and a.answer.questionIndex = :questionIndex")
    List<GroupInterviewAnswerAnalysisEntity> findAllForComparison(
            @Param("roomId") Long roomId,
            @Param("questionIndex") Integer questionIndex
    );
}
