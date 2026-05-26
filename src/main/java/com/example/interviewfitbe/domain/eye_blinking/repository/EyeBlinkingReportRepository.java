package com.example.interviewfitbe.domain.eye_blinking.repository;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EyeBlinkingReportRepository extends JpaRepository<EyeBlinkingReport, Long> {
}