package com.example.interviewfitbe.domain.video_storage.repository;

import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import com.example.interviewfitbe.domain.video_storage.entity.VideoType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<VideoEntity, Long> {

    List<VideoEntity> findAllByInterviewId(Long interviewId);

    List<VideoEntity> findAllByInterviewIdAndVideoType(
            Long interviewId,
            VideoType videoType
    );

    Optional<VideoEntity> findTopByInterviewIdAndVideoTypeOrderByCreatedAtDesc(
            Long interviewId,
            VideoType videoType
    );
}
