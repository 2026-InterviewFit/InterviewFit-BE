package com.example.interviewfitbe.domain.eye_gaze.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_gaze_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeGazeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id")
    private InterviewAnswerEntity interviewAnswer;

    @Column(name = "video_url", nullable = false, length = 1000)
    private String videoUrl;

    @Column(name = "center_ratio")
    private Double centerRatio;

    @Column(name = "gaze_away_ratio")
    private Double gazeAwayRatio;

    @Column(name = "gaze_away_count")
    private Integer gazeAwayCount;

    @Column(name = "score")
    private Double score;

    @Column(name = "evaluation", columnDefinition = "TEXT")
    private String evaluation;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
