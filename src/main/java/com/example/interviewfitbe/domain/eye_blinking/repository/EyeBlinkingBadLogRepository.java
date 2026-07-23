package com.example.interviewfitbe.domain.eye_blinking.repository;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingBadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EyeBlinkingBadLogRepository extends JpaRepository<EyeBlinkingBadLog, Long> {

    List<EyeBlinkingBadLog> findByEyeBlinkingReport_IdOrderByTimestampAsc(Long eyeBlinkingReportId);
}