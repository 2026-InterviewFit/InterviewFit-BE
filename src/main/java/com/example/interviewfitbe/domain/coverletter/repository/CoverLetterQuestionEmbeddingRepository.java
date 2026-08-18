package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEmbeddingEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CoverLetterQuestionEmbeddingRepository extends JpaRepository<CoverLetterQuestionEmbeddingEntity, Long> {
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO cover_letter_question_embeddings(cover_letter_question_id, embedding)
        VALUES(:questionId, CAST(:embedding AS vector))
    """, nativeQuery = true)
    void saveEmbedding(@Param("questionId") Long questionId, @Param("embedding") String embedding);

    @Query(value = """
        SELECT cle.*
        FROM cover_letter_question_embeddings cle
        JOIN cover_letter_questions clq
            ON cle.cover_letter_question_id = clq.id
        JOIN cover_letters cl
            ON clq.cover_letter_id = cl.id
        WHERE cl.user_id = :userId
          AND cle.embedding <=> CAST(:vectorEmbedding AS vector) <= 0.1
        ORDER BY cle.embedding <=> CAST(:vectorEmbedding AS vector)
        LIMIT :limit
    """, nativeQuery = true)
    List<CoverLetterQuestionEmbeddingEntity> findSimilarByUser(
            @Param("userId") Long userId,
            @Param("vectorEmbedding") String vectorEmbedding,
            @Param("limit") int limit
    );

    List<CoverLetterQuestionEmbeddingEntity> findAllByCoverLetterQuestionIn(List<CoverLetterQuestionEntity> questions);
}
