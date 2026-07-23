package com.example.interviewfitbe.domain.interview.repository;

import com.example.interviewfitbe.domain.interview.entity.InterviewTimelineEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewTimelineEventRepository extends JpaRepository<InterviewTimelineEventEntity, Long> {

    List<InterviewTimelineEventEntity> findByInterview_IdOrderByTimestampAsc(Long interviewId);
}