package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverLetterQuestionRepository extends JpaRepository<CoverLetterQuestionEntity, Long> {
    List<CoverLetterQuestionEntity> findAllByCoverLetter(CoverLetterEntity coverLetter);
}
