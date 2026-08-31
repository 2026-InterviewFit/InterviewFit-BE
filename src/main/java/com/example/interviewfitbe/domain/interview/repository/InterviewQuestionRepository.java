package com.example.interviewfitbe.domain.interview.repository;

import com.example.interviewfitbe.domain.interview.entity.InterviewQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository
        extends JpaRepository<InterviewQuestionEntity, Long> {

    Optional<InterviewQuestionEntity> findByInterview_IdAndQuestion_Id(
            Long interviewId,
            Long questionId
    );

    List<InterviewQuestionEntity> findAllByInterview_IdOrderByQuestionOrderAsc(
            Long interviewId
    );

    boolean existsByInterview_IdAndQuestion_Id(
            Long interviewId,
            Long questionId
    );
}
