package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PtDocumentRepository
        extends JpaRepository<PtDocumentEntity, Long> {

    Optional<PtDocumentEntity> findByPtInterviewId(Long ptInterviewId);
}
