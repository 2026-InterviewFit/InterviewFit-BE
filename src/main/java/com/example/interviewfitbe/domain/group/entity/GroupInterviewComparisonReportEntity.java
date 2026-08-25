package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "group_interview_comparison_reports")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInterviewComparisonReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "participant_user_id", nullable = false)
    private Long participantUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupComparisonScope scope;

    @Column(name = "question_index")
    private Integer questionIndex;

    @Column(name = "content_score", precision = 5, scale = 2)
    private BigDecimal contentScore;
    @Column(name = "speech_score", precision = 5, scale = 2)
    private BigDecimal speechScore;
    @Column(name = "gaze_score", precision = 5, scale = 2)
    private BigDecimal gazeScore;
    @Column(name = "pose_score", precision = 5, scale = 2)
    private BigDecimal poseScore;
    @Column(name = "blink_score", precision = 5, scale = 2)
    private BigDecimal blinkScore;
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "rank_number")
    private Integer rank;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(name = "strength_summary", columnDefinition = "TEXT")
    private String strengthSummary;
    @Column(name = "improvement_summary", columnDefinition = "TEXT")
    private String improvementSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupComparisonStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void updateScores(
            BigDecimal contentScore, BigDecimal speechScore, BigDecimal gazeScore,
            BigDecimal poseScore, BigDecimal blinkScore, BigDecimal totalScore,
            Integer rank, int participantCount, GroupComparisonStatus status
    ) {
        this.contentScore = contentScore;
        this.speechScore = speechScore;
        this.gazeScore = gazeScore;
        this.poseScore = poseScore;
        this.blinkScore = blinkScore;
        this.totalScore = totalScore;
        this.rank = rank;
        this.participantCount = participantCount;
        this.status = status;
    }

    public void updateFeedback(String strengthSummary, String improvementSummary) {
        this.strengthSummary = strengthSummary;
        this.improvementSummary = improvementSummary;
    }
}
