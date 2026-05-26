package com.example.interviewfitbe.domain.followup.repository;

import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpQuestionRepository extends JpaRepository<FollowUpQuestion, Long> {
}