package com.example.interviewfitbe.domain.video_storage.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 영상이 속한 면접
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    // 사용자가 업로드한 원본 파일명
    @Column(name = "original_file_name")
    private String originalFileName;

    // MinIO 등에 저장된 실제 object key
    @Column(name = "object_key", nullable = false)
    private String objectKey;

    // 일반 면접 전체 영상 / PT 발표 영상 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "video_type", nullable = false)
    private VideoType videoType;

    // 영상 길이(초)
    @Column(name = "duration_seconds", nullable = false)
    private Double durationSeconds;

    // video/mp4 등의 MIME 타입
    @Column(name = "content_type", nullable = false)
    private String contentType;

    // 파일 크기(byte)
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}