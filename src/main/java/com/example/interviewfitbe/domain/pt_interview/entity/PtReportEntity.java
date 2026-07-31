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

    /*
     * PT 면접 1개당 종합 리포트 1개
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pt_interview_id",
            nullable = false,
            unique = true
    )
    private PtInterviewEntity ptInterview;

    /*
     * 발표 내용 점수
     */
    private Double contentScore;

    /*
     * 발표 구성 및 논리 점수
     */
    private Double structureScore;

    /*
     * 전달력 점수
     */
    private Double deliveryScore;

    /*
     * 질의응답 대응 점수
     */
    private Double qaScore;

    /*
     * PT 최종 점수
     */
    private Double overallScore;

    /*
     * 발표 내용 전체 요약
     */
    @Column(columnDefinition = "TEXT")
    private String presentationSummary;

    /*
     * 잘한 점
     */
    @Column(columnDefinition = "TEXT")
    private String strengths;

    /*
     * 부족한 점
     */
    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    /*
     * 개선 방향
     */
    @Column(columnDefinition = "TEXT")
    private String improvement;

    /*
     * AI 원본 분석 JSON
     */
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static PtReportEntity create(
            PtInterviewEntity ptInterview,
            Double contentScore,
            Double structureScore,
            Double deliveryScore,
            Double qaScore,
            Double overallScore,
            String presentationSummary,
            String strengths,
            String weaknesses,
            String improvement,
            String resultJson
    ) {

        PtReportEntity entity =
                new PtReportEntity();

        entity.ptInterview =
                ptInterview;

        entity.applyReportResult(
                contentScore,
                structureScore,
                deliveryScore,
                qaScore,
                overallScore,
                presentationSummary,
                strengths,
                weaknesses,
                improvement,
                resultJson
        );

        LocalDateTime now =
                LocalDateTime.now();

        entity.createdAt = now;
        entity.updatedAt = now;

        return entity;
    }

    /*
     * 기존 PT 리포트 재생성 시 내용 갱신
     */
    public void update(
            Double contentScore,
            Double structureScore,
            Double deliveryScore,
            Double qaScore,
            Double overallScore,
            String presentationSummary,
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
                presentationSummary,
                strengths,
                weaknesses,
                improvement,
                resultJson
        );

        this.updatedAt =
                LocalDateTime.now();
    }

    private void applyReportResult(
            Double contentScore,
            Double structureScore,
            Double deliveryScore,
            Double qaScore,
            Double overallScore,
            String presentationSummary,
            String strengths,
            String weaknesses,
            String improvement,
            String resultJson
    ) {

        this.contentScore =
                contentScore;

        this.structureScore =
                structureScore;

        this.deliveryScore =
                deliveryScore;

        this.qaScore =
                qaScore;

        this.overallScore =
                overallScore;

        this.presentationSummary =
                normalizeText(
                        presentationSummary
                );

        this.strengths =
                normalizeText(
                        strengths
                );

        this.weaknesses =
                normalizeText(
                        weaknesses
                );

        this.improvement =
                normalizeText(
                        improvement
                );

        this.resultJson =
                normalizeText(
                        resultJson
                );
    }

    private static String normalizeText(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }
}