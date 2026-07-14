package com.example.interviewfitbe.domain.eye_gaze.repository;

import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EyeGazeReportRepository
        extends JpaRepository<EyeGazeReport, Long> {

    List<EyeGazeReport> findAllByInterviewId(Long interviewId);

    List<EyeGazeReport> findAllByInterviewAnswerId(Long interviewAnswerId);

    Optional<EyeGazeReport> findTopByInterviewIdOrderByCreatedAtDesc(
            Long interviewId
    );

    Optional<EyeGazeReport> findTopByInterviewAnswerIdOrderByIdDesc(
            Long interviewAnswerId
    );
}