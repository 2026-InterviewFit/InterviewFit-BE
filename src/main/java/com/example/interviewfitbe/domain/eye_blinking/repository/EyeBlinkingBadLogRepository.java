package com.example.interviewfitbe.domain.eye_blinking.repository;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingBadLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EyeBlinkingBadLogRepository extends JpaRepository<EyeBlinkingBadLogEntity, Long> {

    List<EyeBlinkingBadLogEntity> findByEyeBlinkingReport_IdOrderByTimestampAsc(Long eyeBlinkingReportId);
}