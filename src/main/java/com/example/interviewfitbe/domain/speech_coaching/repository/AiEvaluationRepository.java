package com.example.interviewfitbe.domain.speech_coaching.repository;

import com.example.interviewfitbe.domain.speech_coaching.entity.AiEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluationEntity, Long> {

    /**
     * 질문별 AI 평가 조회
     */
    Optional<AiEvaluationEntity> findByInterviewAnswer_Id(Long interviewAnswerId);

}