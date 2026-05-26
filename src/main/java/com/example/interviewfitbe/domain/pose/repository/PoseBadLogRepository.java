package com.example.interviewfitbe.domain.pose.repository;

import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoseBadLogRepository extends JpaRepository<PoseBadLogEntity, Long> {
}
