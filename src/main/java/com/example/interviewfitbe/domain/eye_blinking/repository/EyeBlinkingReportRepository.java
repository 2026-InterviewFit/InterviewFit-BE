package com.example.interviewfitbe.domain.eye_blinking.repository;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EyeBlinkingReportRepository extends JpaRepository<EyeBlinkingReportEntity, Long> {

    Optional<EyeBlinkingReportEntity> findTopByInterview_IdOrderByCreatedAtDesc(Long interviewId);

    Optional<EyeBlinkingReportEntity> findTopByInterviewAnswer_IdOrderByCreatedAtDesc(Long interviewAnswerId);

    List<EyeBlinkingReportEntity> findByInterview_IdOrderByCreatedAtAsc(Long interviewId);
}