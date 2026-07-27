package com.example.interviewfitbe.domain.pt_interview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pt_slide",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pt_slide_document_number",
                        columnNames = {
                                "pt_document_id",
                                "slide_number"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtSlideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 하나의 PPT에는 여러 슬라이드가 존재
     *
     * PtDocument 1 : N PtSlide
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pt_document_id",
            nullable = false
    )
    private PtDocumentEntity document;

    /*
     * 슬라이드 번호
     */
    @Column(nullable = false)
    private Integer slideNumber;

    /*
     * AI가 판단하거나 PPT에서 추출한 제목
     */
    private String title;

    /*
     * PPT에서 추출한 원본 텍스트
     */
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    /*
     * AI가 생성한 해당 슬라이드 요약
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /*
     * 슬라이드 이미지 MinIO ObjectKey
     */
    private String imageObjectKey;

    /*
     * 추가 분석 결과(JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String analysisJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 일반적인 PT 문서 분석 시 사용하는 생성 메서드
     */
    public static PtSlideEntity create(
            PtDocumentEntity document,
            Integer slideNumber,
            String title,
            String extractedText,
            String summary
    ) {
        return create(
                document,
                slideNumber,
                title,
                extractedText,
                summary,
                null,
                null
        );
    }

    /**
     * 이미지 분석 등 추가 정보가 있을 때 사용하는 생성 메서드
     */
    public static PtSlideEntity create(
            PtDocumentEntity document,
            Integer slideNumber,
            String title,
            String extractedText,
            String summary,
            String imageObjectKey,
            String analysisJson
    ) {

        PtSlideEntity entity = new PtSlideEntity();

        entity.document = document;
        entity.slideNumber = slideNumber;
        entity.title = title;
        entity.extractedText = extractedText;
        entity.summary = summary;
        entity.imageObjectKey = imageObjectKey;
        entity.analysisJson = analysisJson;
        entity.createdAt = LocalDateTime.now();

        return entity;
    }
}