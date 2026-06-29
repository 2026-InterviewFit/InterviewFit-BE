package com.example.interviewfitbe.domain.VideoStorage.repository;

import com.example.interviewfitbe.domain.VideoStorage.entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository
        extends JpaRepository<VideoEntity, Long> {
}
