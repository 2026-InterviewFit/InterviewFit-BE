package com.example.interviewfitbe.domain.question.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findAllByOrderByCreatedAtDesc();
    List<QuestionEntity> findByCoverLetterInAndType(List<CoverLetterEntity> coverLetter, QuestionType type);
}
