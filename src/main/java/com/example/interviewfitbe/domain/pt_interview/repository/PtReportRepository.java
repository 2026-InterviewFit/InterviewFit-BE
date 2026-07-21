package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PtReportRepository
        extends JpaRepository<PtReportEntity, Long> {

    Optional<PtReportEntity> findByPtInterviewId(Long ptInterviewId);
}
