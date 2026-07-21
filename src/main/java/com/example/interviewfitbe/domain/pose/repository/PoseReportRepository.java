package com.example.interviewfitbe.domain.pose.repository;

import com.example.interviewfitbe.domain.pose.entity.PoseReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PoseReportRepository extends JpaRepository<PoseReportEntity, Long> {
    Optional<PoseReportEntity> findTopByInterview_IdOrderByIdDesc(Long interviewId);
    List<PoseReportEntity> findByInterview_IdOrderByIdAsc(Long interviewId);
}