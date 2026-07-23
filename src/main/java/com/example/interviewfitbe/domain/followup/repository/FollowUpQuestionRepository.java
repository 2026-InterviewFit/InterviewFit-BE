package com.example.interviewfitbe.domain.followup.repository;

import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpQuestionRepository extends JpaRepository<FollowUpQuestionEntity, Long> {

    List<FollowUpQuestionEntity> findByInterview_IdOrderByCreatedAtAsc(Long interviewId);

    List<FollowUpQuestionEntity> findBySourceQuestion_IdOrderByCreatedAtAsc(Long sourceQuestionId);

    List<FollowUpQuestionEntity> findBySourceAnswer_IdOrderByCreatedAtAsc(Long sourceAnswerId);
}