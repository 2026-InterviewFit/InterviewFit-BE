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

    @Query(value = """
            SELECT ga.id AS "answerId",
                   ga.round_number AS "roundNumber",
                   ga.question_index AS "questionIndex",
                   ga.group_question_id AS "questionId",
                   gq.question_text AS "questionText",
                   ga.answer_started_at AS "answerStartedAt",
                   ga.answer_completed_at AS "answerCompletedAt",
                   ga.completion_type AS "completionType",
                   (ga.video_object_key IS NOT NULL) AS "videoUploaded",
                   gaa.status AS "analysisStatus"
            FROM group_interview_answers ga
            LEFT JOIN group_questions gq ON gq.id = ga.group_question_id
            LEFT JOIN group_interview_answer_analyses gaa
                   ON gaa.group_interview_answer_id = ga.id
            WHERE ga.group_interview_id = :roomId
              AND ga.participant_user_id = :userId
            ORDER BY ga.round_number ASC, ga.question_index ASC, ga.id ASC
            """, nativeQuery = true)
    List<GroupInterviewMyAnswerProjection> findMyAnswersWithAnalysis(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );
}
