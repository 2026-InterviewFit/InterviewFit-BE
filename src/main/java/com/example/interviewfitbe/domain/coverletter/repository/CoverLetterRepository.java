package com.example.interviewfitbe.domain.coverletter.repository;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoverLetterRepository extends JpaRepository<CoverLetterEntity, Long> {
    Optional<CoverLetterEntity> findByUserAndContent(UserEntity user, String content);
}
