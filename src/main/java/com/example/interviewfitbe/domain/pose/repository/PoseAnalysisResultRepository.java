package com.example.interviewfitbe.domain.pose.repository;

import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PoseAnalysisResultRepository extends JpaRepository<PoseAnalysisResultEntity, Long> {
    Optional<PoseAnalysisResultEntity> findTopByInterview_IdOrderByIdDesc(Long interviewId);
    Optional<PoseAnalysisResultEntity> findTopByInterviewAnswer_IdOrderByIdDesc(Long interviewAnswerId);
    List<PoseAnalysisResultEntity> findByInterview_IdOrderByIdAsc(Long interviewId);
}