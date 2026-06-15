package com.example.interviewfitbe.domain.speech_coaching.repository;

import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpeechReportRepository extends JpaRepository<SpeechReportEntity, Long> {

    Optional<SpeechReportEntity> findTopByInterview_IdOrderByCreatedAtDesc(Long interviewId);

    Optional<SpeechReportEntity> findTopByInterviewAnswer_IdOrderByCreatedAtDesc(Long interviewAnswerId);

    List<SpeechReportEntity> findByInterview_IdOrderByCreatedAtAsc(Long interviewId);
}