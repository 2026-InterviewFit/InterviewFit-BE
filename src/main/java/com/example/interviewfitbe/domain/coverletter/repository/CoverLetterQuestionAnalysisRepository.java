package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverLetterQuestionAnalysisRepository extends JpaRepository<CoverLetterQuestionAnalysisEntity, Long> {
    List<CoverLetterQuestionAnalysisEntity> findAllByCoverLetterQuestionIn(List<CoverLetterQuestionEntity> coverLetterQuestions);
}
