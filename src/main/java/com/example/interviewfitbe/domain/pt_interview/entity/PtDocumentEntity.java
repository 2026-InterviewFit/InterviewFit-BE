package com.example.interviewfitbe.domain.pt_interview.entity;

import com.example.interviewfitbe.domain.pt_interview.enums.PtDocumentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pt_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pt_interview_id",
            nullable = false,
            unique = true
    )
    private PtInterviewEntity ptInterview;

    private String originalFileName;

    @Column(nullable = false)
    private String objectKey;

    private String contentType;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PtDocumentStatus analysisStatus;

    /*
     * 전체 슬라이드 수
     */
    private Integer totalSlides;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PtDocumentEntity create(
            PtInterviewEntity ptInterview,
            String originalFileName,
            String objectKey,
            String contentType,
            Long fileSize
    ) {
        PtDocumentEntity entity = new PtDocumentEntity();

        entity.ptInterview = ptInterview;
        entity.originalFileName = originalFileName;
        entity.objectKey = objectKey;
        entity.contentType = contentType;
        entity.fileSize = fileSize;
        entity.analysisStatus = PtDocumentStatus.UPLOADED;
        entity.totalSlides = 0;
        entity.createdAt = LocalDateTime.now();

        return entity;
    }

    public void startAnalysis() {
        this.analysisStatus = PtDocumentStatus.ANALYZING;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeAnalysis(
            String summary,
            Integer totalSlides
    ) {
        this.analysisStatus = PtDocumentStatus.COMPLETED;
        this.summary = summary;
        this.totalSlides = totalSlides;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void failAnalysis(String errorMessage) {
        this.analysisStatus = PtDocumentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}