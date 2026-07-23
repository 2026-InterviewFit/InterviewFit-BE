package com.example.interviewfitbe.domain.eye_gaze.repository;

import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeBadLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EyeGazeBadLogRepository
        extends JpaRepository<EyeGazeBadLogEntity, Long> {

    List<EyeGazeBadLogEntity> findAllByEyeGazeReportId(Long eyeGazeReportId);
}
