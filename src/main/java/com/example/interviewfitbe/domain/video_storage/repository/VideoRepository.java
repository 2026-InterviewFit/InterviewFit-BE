package com.example.interviewfitbe.domain.video_storage.repository;

import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository
        extends JpaRepository<VideoEntity, Long> {
}
