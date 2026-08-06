package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface GroupInterviewSessionRepository
        extends JpaRepository<GroupInterviewSessionEntity, Long> {

    Optional<GroupInterviewSessionEntity>
    findByInterviewId(Long interviewId);

}