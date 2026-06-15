package com.example.interviewfitbe.domain.interview.repository;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswerEntity, Long> {

    List<InterviewAnswerEntity> findByInterview_IdOrderByAnswerOrderAsc(Long interviewId);

    @EntityGraph(attributePaths = {"interview"})
    Optional<InterviewAnswerEntity> findWithInterviewById(Long id);
}