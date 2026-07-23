package com.example.interviewfitbe.domain.eye_gaze.entity;

import com.example.interviewfitbe.domain.eye_gaze.enums.GazeDirection;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_gaze_bad_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeGazeBadLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 시선 이탈 로그가 속한 시선 분석 결과
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eye_gaze_report_id", nullable = false)
    private EyeGazeReport eyeGazeReport;

    /**
     * 시선 이탈이 시작된 시점 (초)
     */
    @Column(name = "timestamp", nullable = false)
    private Double timestamp;

    /**
     * 시선 이탈이 지속된 시간 (초)
     */
    @Column(name = "duration")
    private Double duration;

    /**
     * 시선이 벗어난 방향
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private GazeDirection direction;

    /**
     * 정면 기준에서 벗어난 정도
     * 0 ~ 1 정규화 값
     */
    @Column(name = "deviation")
    private Double deviation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}