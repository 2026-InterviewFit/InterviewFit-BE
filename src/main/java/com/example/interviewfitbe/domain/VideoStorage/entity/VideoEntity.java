package com.example.interviewfitbe.domain.VideoStorage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class VideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;

    private String objectKey;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}