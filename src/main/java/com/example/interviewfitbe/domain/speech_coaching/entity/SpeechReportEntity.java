package com.example.interviewfitbe.domain.speech_coaching.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "speech_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpeechReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 면접 하나당 하나의 스피치 리포트
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false, unique = true)
    private InterviewEntity interview;

    /**
     * 전체 STT 결과
     */
    @Column(columnDefinition = "TEXT")
    private String textScript;

    /**
     * 필러 분석
     */
    private Integer totalFillerCount;

    @Column(columnDefinition = "TEXT")
    private String fillerDetailsJson;

    /**
     * 반복 표현 분석
     */
    @Column(columnDefinition = "TEXT")
    private String repeatedExpressionsJson;

    /**
     * 말하기 속도 분석
     */
    private String paceEvaluation;

    @Column(columnDefinition = "TEXT")
    private String paceFeedback;

    private Double charactersPerSecond;

    /**
     * 침묵 분석
     */
    private Double totalSilenceSeconds;

    private Double pauseRatePercent;

    private Integer criticalSilenceCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}