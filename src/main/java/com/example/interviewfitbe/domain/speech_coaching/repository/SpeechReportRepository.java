// package com.example.interviewfitbe.domain.speech_coaching.repository;
package com.example.interviewfitbe.domain.speech_coaching.repository;

import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechReportRepository extends JpaRepository<SpeechReport, Long> {
}