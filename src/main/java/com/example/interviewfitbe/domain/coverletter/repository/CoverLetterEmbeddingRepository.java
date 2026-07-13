package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEmbeddingEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoverLetterEmbeddingRepository extends JpaRepository<CoverLetterEmbeddingEntity, Long> {
    @Modifying
    @Query(value = """
        INSERT INTO cover_letter_embeddings(cover_letter_id, embedding) 
        VALUES (:coverLetterId, CAST(:embedding AS vector))
        """, nativeQuery = true)
    void saveEmbedding(@Param("coverLetterId") Long coverLetterId, @Param("embedding") String embedding);

    @Query(value = """
        SELECT cle.*
        FROM cover_letter_embeddings cle
        JOIN cover_letters cl
            ON cle.cover_letter_id = cl.id
        WHERE cl.user_id = :userId
        ORDER BY cle.embedding <=> CAST(:vectorEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<CoverLetterEmbeddingEntity> findSimilarByUser(@Param("userId") Long userId, @Param("vectorEmbedding") String vectorEmbedding, @Param("limit") int limit);

    Optional<CoverLetterEmbeddingEntity> findByCoverLetter(CoverLetterEntity coverLetter);
}
