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
          AND q.interview IS NULL
          AND q.coverLetterQuestion IS NULL
          AND q.createdAt <= :expiredAt
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
        SELECT *
        FROM questions
        WHERE interview_id IS NULL
          AND cover_letter_question_id IN (:coverLetterQuestionIds)
          AND type = :type
        ORDER BY created_at
        LIMIT :count
    """, nativeQuery = true)
    List<QuestionEntity> findReusableQuestions(@Param("coverLetterQuestionIds") List<Long> coverLetterQuestionIds, @Param("type") String type, @Param("count") int count);

    List<QuestionEntity> findAllByOrderByCreatedAtDesc();
    List<QuestionEntity> findTop6ByCoverLetterQuestionInAndTypeOrderByCreatedAtDesc(List<CoverLetterQuestionEntity> coverLetterQuestions, QuestionType type);
    List<QuestionEntity> findTop6ByCoverLetterQuestionInAndTypeAndIdNotInOrderByCreatedAtDesc(List<CoverLetterQuestionEntity> coverLetterQuestions, QuestionType type, List<Long> excludeIds);
}
