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

    /**
     * 실제 면접에서 한 번도 사용되지 않은 오래된 DEFAULT 질문만 정리한다.
     *
     * QuestionEntity.interview가 아니라 interview_questions 연결 테이블을 기준으로
     * 사용 여부를 판단한다.
     */
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
    List<Long> findExpiredQuestionIds(
            @Param("type") QuestionType type,
            @Param("expiredAt") LocalDateTime expiredAt,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM QuestionEntity q
        WHERE q.id IN :ids
    """)
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * AI 생성 질문 원본은 특정 Interview에 귀속되지 않는다.
     * 따라서 interview_id 여부와 관계없이 자기소개서 문항 + 타입 기준으로 재사용한다.
     */
    @Query(value = """
        SELECT *
        FROM questions
        WHERE cover_letter_question_id IN (:coverLetterQuestionIds)
          AND type = :type
        ORDER BY created_at
        LIMIT :count
    """, nativeQuery = true)
    List<QuestionEntity> findReusableQuestions(
            @Param("coverLetterQuestionIds") List<Long> coverLetterQuestionIds,
            @Param("type") String type,
            @Param("count") int count
    );

    List<QuestionEntity> findAllByOrderByCreatedAtDesc();

    List<QuestionEntity> findTop6ByCoverLetterQuestionInAndTypeOrderByCreatedAtDesc(
            List<CoverLetterQuestionEntity> coverLetterQuestions,
            QuestionType type
    );

    List<QuestionEntity> findTop6ByCoverLetterQuestionInAndTypeAndIdNotInOrderByCreatedAtDesc(
            List<CoverLetterQuestionEntity> coverLetterQuestions,
            QuestionType type,
            List<Long> excludeIds
    );
}
