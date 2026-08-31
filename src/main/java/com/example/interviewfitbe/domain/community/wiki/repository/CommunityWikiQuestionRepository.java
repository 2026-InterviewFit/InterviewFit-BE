package com.example.interviewfitbe.domain.community.wiki.repository;

import com.example.interviewfitbe.domain.community.wiki.entity.CommunityWikiQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityWikiQuestionRepository
        extends JpaRepository<CommunityWikiQuestionEntity, Long> {

    List<CommunityWikiQuestionEntity> findAllByOrderByCreatedAtDesc();
}
