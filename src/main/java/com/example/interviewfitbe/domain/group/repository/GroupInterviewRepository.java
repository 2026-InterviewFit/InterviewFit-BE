package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupInterviewRepository extends JpaRepository<GroupInterviewEntity, Long> {

    @Query(value = """
            WITH participant_counts AS (
                SELECT room_id, COUNT(*) AS participant_count
                FROM group_participants
                GROUP BY room_id
            ),
            my_analysis_stats AS (
                SELECT ga.group_interview_id,
                       COUNT(*) AS total_answers,
                       COUNT(*) FILTER (WHERE gaa.status = 'UPLOADED') AS uploaded_analyses,
                       COUNT(*) FILTER (WHERE gaa.status = 'RUNNING') AS running_analyses,
                       COUNT(*) FILTER (WHERE gaa.status = 'DONE') AS completed_analyses,
                       COUNT(*) FILTER (WHERE gaa.status = 'FAILED') AS failed_analyses
                FROM group_interview_answers ga
                LEFT JOIN group_interview_answer_analyses gaa
                       ON gaa.group_interview_answer_id = ga.id
                WHERE ga.participant_user_id = :userId
                GROUP BY ga.group_interview_id
            )
            SELECT gi.id AS "roomId",
                   gi.title AS "title",
                   gi.job_role_id AS "jobRoleId",
                   jr.category AS "jobCategory",
                   jr.name AS "jobName",
                   gi.started_at AS "startedAt",
                   gi.ended_at AS "endedAt",
                   gi.room_status AS "roomStatus",
                   COALESCE(pc.participant_count, 0) AS "participantCount",
                   COALESCE(ms.total_answers, 0) AS "totalAnswers",
                   COALESCE(ms.uploaded_analyses, 0) AS "uploadedAnalyses",
                   COALESCE(ms.running_analyses, 0) AS "runningAnalyses",
                   COALESCE(ms.completed_analyses, 0) AS "completedAnalyses",
                   COALESCE(ms.failed_analyses, 0) AS "failedAnalyses"
            FROM group_interviews gi
            JOIN group_participants gp
              ON gp.room_id = gi.id AND gp.user_id = :userId
            LEFT JOIN job_roles jr ON jr.id = gi.job_role_id
            LEFT JOIN participant_counts pc ON pc.room_id = gi.id
            LEFT JOIN my_analysis_stats ms ON ms.group_interview_id = gi.id
            WHERE gi.room_status = 'COMPLETED'
            ORDER BY gi.ended_at DESC, gi.id DESC
            """, nativeQuery = true)
    List<GroupInterviewHistoryProjection> findCompletedHistoryByUserId(
            @Param("userId") Long userId
    );
}
