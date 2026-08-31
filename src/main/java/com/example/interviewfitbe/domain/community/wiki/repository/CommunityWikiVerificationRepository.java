package com.example.interviewfitbe.domain.community.wiki.repository;

import com.example.interviewfitbe.domain.community.wiki.entity.CommunityWikiVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityWikiVerificationRepository
        extends JpaRepository<CommunityWikiVerificationEntity, Long> {

    boolean existsByWikiQuestion_IdAndUser_Id(
            Long wikiQuestionId,
            Long userId
    );
}
