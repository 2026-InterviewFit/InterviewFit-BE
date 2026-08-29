package com.example.interviewfitbe.domain.community.answer.repository;

import com.example.interviewfitbe.domain.community.answer.entity.CommunityAnswerLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityAnswerLikeRepository
        extends JpaRepository<CommunityAnswerLikeEntity, Long> {

    boolean existsByBestAnswer_IdAndUser_Id(
            Long bestAnswerId,
            Long userId
    );

    Optional<CommunityAnswerLikeEntity> findByBestAnswer_IdAndUser_Id(
            Long bestAnswerId,
            Long userId
    );

    @Query("""
            select l.bestAnswer.id
            from CommunityAnswerLikeEntity l
            where l.user.id = :userId
            """)
    List<Long> findBestAnswerIdsLikedByUser(
            @Param("userId") Long userId
    );
}
