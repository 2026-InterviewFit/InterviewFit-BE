package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterProcessingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoverLetterProcessingRepository extends JpaRepository<CoverLetterProcessingEntity, Long> {
    Optional<CoverLetterProcessingEntity> findByCoverLetter(CoverLetterEntity coverLetter);
}
