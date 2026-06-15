package com.example.interviewfitbe.domain.eye_blinking.repository;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EyeBlinkingReportRepository extends JpaRepository<EyeBlinkingReport, Long> {

    Optional<EyeBlinkingReport> findTopByInterview_IdOrderByCreatedAtDesc(Long interviewId);

    Optional<EyeBlinkingReport> findTopByInterviewAnswer_IdOrderByCreatedAtDesc(Long interviewAnswerId);

    List<EyeBlinkingReport> findByInterview_IdOrderByCreatedAtAsc(Long interviewId);
}