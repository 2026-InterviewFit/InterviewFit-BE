package com.example.interviewfitbe.domain.job.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.job.entity.RecommendedJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendedJobRepository extends JpaRepository<RecommendedJobEntity, Long> {
    Optional<RecommendedJobEntity> findByCoverLetter(CoverLetterEntity coverLetter);
}
