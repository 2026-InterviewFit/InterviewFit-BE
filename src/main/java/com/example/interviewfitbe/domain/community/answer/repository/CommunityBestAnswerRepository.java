package com.example.interviewfitbe.domain.community.answer.repository;

import com.example.interviewfitbe.domain.community.answer.entity.CommunityBestAnswerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityBestAnswerRepository
        extends JpaRepository<CommunityBestAnswerEntity, Long> {

    @EntityGraph(attributePaths = {"user", "jobRole"})
    List<CommunityBestAnswerEntity> findAllByOrderByCreatedAtDesc();

    boolean existsBySourceAnswerId(Long sourceAnswerId);
}
