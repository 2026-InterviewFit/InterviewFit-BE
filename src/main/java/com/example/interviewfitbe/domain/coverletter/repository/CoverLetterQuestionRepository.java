package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoverLetterQuestionRepository extends JpaRepository<CoverLetterQuestionEntity, Long> {
    @Query(value = """
        SELECT q.*
        FROM cover_letter_questions q
        LEFT JOIN cover_letter_question_embeddings e
            ON q.id = e.cover_letter_question_id
        WHERE q.cover_letter_id = :coverLetterId
          AND e.cover_letter_question_id IS NULL
    """, nativeQuery = true)
    List<CoverLetterQuestionEntity> findQuestionsWithoutEmbedding(@Param("coverLetterId") Long coverLetterId);

    List<CoverLetterQuestionEntity> findAllByCoverLetter(CoverLetterEntity coverLetter);
}
