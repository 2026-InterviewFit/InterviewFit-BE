package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public interface GroupInterviewSessionRepository
        extends JpaRepository<GroupInterviewSessionEntity, Long> {

    Optional<GroupInterviewSessionEntity>
    findByInterviewId(Long interviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GroupInterviewSessionEntity s where s.interview.id = :interviewId")
    Optional<GroupInterviewSessionEntity> findByInterviewIdForUpdate(
            @Param("interviewId") Long interviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GroupInterviewSessionEntity s where s.id = :sessionId")
    Optional<GroupInterviewSessionEntity> findByIdForUpdate(
            @Param("sessionId") Long sessionId);

    @Query("select s.id from GroupInterviewSessionEntity s " +
            "where s.status = com.example.interviewfitbe.domain.group.entity.InterviewStatus.ANSWERING " +
            "and s.answerDeadlineAt <= :now")
    List<Long> findExpiredAnswerSessionIds(@Param("now") Instant now);

    @Query("select s.id from GroupInterviewSessionEntity s " +
            "where s.status = com.example.interviewfitbe.domain.group.entity.InterviewStatus.QUESTION_VIEW " +
            "and s.answerStartDeadlineAt <= :now")
    List<Long> findExpiredAnswerStartSessionIds(@Param("now") Instant now);

}
