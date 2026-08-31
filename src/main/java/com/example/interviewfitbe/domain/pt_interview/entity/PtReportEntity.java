package com.example.interviewfitbe.domain.pt_interview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pt_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtReportEntity {

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

    private Integer contentScore;
    private Integer structureScore;
    private Integer deliveryScore;
    private Integer qaScore;
    private Integer overallScore;

    /**
     * 업로드한 PPT/PDF 자체 요약
     */
    @Column(columnDefinition = "TEXT")
    private String presentationMaterialSummary;

    /**
     * 사용자가 실제 발표한 내용 요약
     */
    @Column(columnDefinition = "TEXT")
    private String actualPresentationSummary;

    /**
     * 발표자료와 실제 발표 비교 분석
     */
    @Column(columnDefinition = "TEXT")
    private String presentationComparison;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String improvement;

    /**
     * 질문별 상세 분석 JSON
     */
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static PtReportEntity create(
            PtInterviewEntity ptInterview,
            Integer contentScore,
            Integer structureScore,
            Integer deliveryScore,
            Integer qaScore,
            Integer overallScore,
            String presentationMaterialSummary,
            String actualPresentationSummary,
            String presentationComparison,
            String strengths,
            String weaknesses,
            String improvement,
            String resultJson
    ) {

        PtReportEntity entity = new PtReportEntity();

        entity.ptInterview = ptInterview;

        entity.applyReportResult(
                contentScore,
                structureScore,
                deliveryScore,
                qaScore,
                overallScore,
                presentationMaterialSummary,
                actualPresentationSummary,
                presentationComparison,
                strengths,
                weaknesses,
                improvement,
                resultJson
        );

        LocalDateTime now = LocalDateTime.now();

        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    public void update(
            Integer contentScore,
            Integer structureScore,
            Integer deliveryScore,
            Integer qaScore,
            Integer overallScore,
            String presentationMaterialSummary,
            String actualPresentationSummary,
            String presentationComparison,
            String strengths,
            String weaknesses,
            String improvement,
            String resultJson
    ) {

        applyReportResult(
                contentScore,
                structureScore,
                deliveryScore,
                qaScore,
                overallScore,
                presentationMaterialSummary,
                actualPresentationSummary,
                presentationComparison,
                strengths,
                weaknesses,
                improvement,
                resultJson
        );

        this.updatedAt = LocalDateTime.now();
    }

    private void applyReportResult(
            Integer contentScore,
            Integer structureScore,
            Integer deliveryScore,
            Integer qaScore,
            Integer overallScore,
            String presentationMaterialSummary,
            String actualPresentationSummary,
            String presentationComparison,
            String strengths,
            String weaknesses,
            String improvement,
            String resultJson
    ) {

        this.contentScore = contentScore;
        this.structureScore = structureScore;
        this.deliveryScore = deliveryScore;
        this.qaScore = qaScore;
        this.overallScore = overallScore;

        this.presentationMaterialSummary =
                normalizeText(presentationMaterialSummary);

        this.actualPresentationSummary =
                normalizeText(actualPresentationSummary);

        this.presentationComparison =
                normalizeText(presentationComparison);

        this.strengths =
                normalizeText(strengths);

        this.weaknesses =
                normalizeText(weaknesses);

        this.improvement =
                normalizeText(improvement);

        this.resultJson =
                normalizeText(resultJson);
    }

    private static String normalizeText(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}