package com.example.interviewfitbe.domain.question.repository;

import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
}
