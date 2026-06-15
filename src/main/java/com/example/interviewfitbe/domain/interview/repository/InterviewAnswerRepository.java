package com.example.interviewfitbe.domain.interview.repository;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswerEntity, Long> {

    List<InterviewAnswerEntity> findByInterview_IdOrderByAnswerOrderAsc(Long interviewId);
}