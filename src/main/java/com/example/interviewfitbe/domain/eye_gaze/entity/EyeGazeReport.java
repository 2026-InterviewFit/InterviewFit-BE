package com.example.interviewfitbe.domain.eye_gaze.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.eye_gaze.enums.EyeGazeGrade;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_gaze_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeGazeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "center_ratio")
    private Double centerRatio;

    @Column(name = "gaze_away_ratio")
    private Double gazeAwayRatio;

    @Column(name = "gaze_away_count")
    private Integer gazeAwayCount;

    @Column(name = "not_detected_count")
    private Integer notDetectedCount;

    @Column(name = "not_detected_ratio")
    private Double notDetectedRatio;

    @Column(name = "score")
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade")
    private EyeGazeGrade grade;

    @Column(name = "evaluation", columnDefinition = "TEXT")
    private String evaluation;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
