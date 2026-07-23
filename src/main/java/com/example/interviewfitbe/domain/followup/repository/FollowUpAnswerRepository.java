package com.example.interviewfitbe.domain.followup.repository;

import com.example.interviewfitbe.domain.followup.entity.FollowUpAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpAnswerRepository extends JpaRepository<FollowUpAnswerEntity, Long> {

    List<FollowUpAnswerEntity> findByFollowUpQuestion_IdOrderByCreatedAtAsc(Long followUpQuestionId);

    List<FollowUpAnswerEntity> findByInterview_IdOrderByCreatedAtAsc(Long interviewId);
}