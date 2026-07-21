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

    /*
     * 하나의 PT 면접당 하나의 발표자료
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pt_interview_id",
            nullable = false,
            unique = true
    )
    private PtInterviewEntity ptInterview;

    /*
     * 사용자가 업로드한 원본 파일명
     */
    private String originalFileName;

    /*
     * MinIO에 저장된 실제 파일 경로
     *
     * 예:
     * pt-documents/15/uuid_presentation.pptx
     */
    @Column(nullable = false)
    private String objectKey;

    /*
     * MIME Type
     *
     * 예:
     * application/vnd.openxmlformats-officedocument.presentationml.presentation
     */
    private String contentType;

    /*
     * 파일 크기(byte)
     */
    private Long fileSize;

    /*
     * PPT 분석 진행 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PtDocumentStatus analysisStatus;

    /*
     * PPT 전체 요약
     *
     * FastAPI + AI 분석 완료 후 저장
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /*
     * 분석 실패 시 오류 내용
     */
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
        entity.createdAt = LocalDateTime.now();

        return entity;
    }

    public void startAnalysis() {
        this.analysisStatus = PtDocumentStatus.ANALYZING;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeAnalysis(String summary) {
        this.analysisStatus = PtDocumentStatus.COMPLETED;
        this.summary = summary;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void failAnalysis(String errorMessage) {
        this.analysisStatus = PtDocumentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}
