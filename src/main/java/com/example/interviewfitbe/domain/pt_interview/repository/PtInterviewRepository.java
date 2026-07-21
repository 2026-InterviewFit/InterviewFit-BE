package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PtInterviewRepository
        extends JpaRepository<PtInterviewEntity, Long> {

    Optional<PtInterviewEntity> findByInterviewId(Long interviewId);
}
