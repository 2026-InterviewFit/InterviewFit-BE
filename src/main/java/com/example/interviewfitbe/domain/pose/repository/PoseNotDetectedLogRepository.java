package com.example.interviewfitbe.domain.pose.repository;

import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoseNotDetectedLogRepository extends JpaRepository<PoseNotDetectedLogEntity, Long> {
}
