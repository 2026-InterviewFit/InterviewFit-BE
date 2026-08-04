package com.example.interviewfitbe.domain.eye_gaze.repository;

import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EyeGazeReportRepository
        extends JpaRepository<EyeGazeReport, Long> {

    List<EyeGazeReport> findAllByInterview_Id(Long interviewId);

    Optional<EyeGazeReport> findTopByInterview_IdOrderByCreatedAtDesc(
            Long interviewId
    );
}