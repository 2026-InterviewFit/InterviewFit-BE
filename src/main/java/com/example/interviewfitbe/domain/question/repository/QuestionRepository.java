package com.example.interviewfitbe.domain.question.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    @Query("""
        SELECT q.id
        FROM QuestionEntity q
        WHERE q.type = :type
          AND q.coverLetterQuestion IS NULL
          AND q.createdAt <= :expiredAt
          AND NOT EXISTS (
              SELECT iq.id
              FROM InterviewQuestionEntity iq
              WHERE iq.question = q
          )
        ORDER BY q.id
    """)
    List<Long> findExpiredQuestionIds(@Param("type") QuestionType type, @Param("expiredAt") LocalDateTime expiredAt, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM QuestionEntity q
        WHERE q.id IN :ids
    """)
    int deleteByIds(@Param("ids") List<Long> ids);

    @Query(value = """
        SELECT q.*
        FROM questions q
        WHERE q.cover_letter_question_id IN (:coverLetterQuestionIds)
          AND q.type = :type
          AND NOT EXISTS (
              SELECT iq.id
              FROM interview_questions iq
              WHERE iq.question_id = q.id
          )
        ORDER BY q.created_at
        LIMIT :count
    """, nativeQuery = true)
    List<QuestionEntity> findReusableQuestions(@Param("coverLetterQuestionIds") List<Long> coverLetterQuestionIds, @Param("type") String type, @Param("count") int count);

    List<QuestionEntity> findAllByOrderByCreatedAtDesc();
    List<QuestionEntity> findTop6ByCoverLetterQuestionInAndTypeOrderByCreatedAtDesc(List<CoverLetterQuestionEntity> coverLetterQuestions, QuestionType type);
    List<QuestionEntity> findTop6ByCoverLetterQuestionInAndTypeAndIdNotInOrderByCreatedAtDesc(List<CoverLetterQuestionEntity> coverLetterQuestions, QuestionType type, List<Long> excludeIds);
}
