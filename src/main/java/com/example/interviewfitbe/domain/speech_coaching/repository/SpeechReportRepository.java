package com.example.interviewfitbe.domain.speech_coaching.repository;

import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpeechReportRepository extends JpaRepository<SpeechReportEntity, Long> {

    /**
     * 면접별 Speech Report 조회
     */
    Optional<SpeechReportEntity> findByInterview_Id(Long interviewId);

}