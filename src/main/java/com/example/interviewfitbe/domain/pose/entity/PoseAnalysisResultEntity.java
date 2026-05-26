package com.example.interviewfitbe.domain.pose.entity;

import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "pose_analysis_results")
public class PoseAnalysisResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer totalDuration;

    // 전체 BAD
    private Integer badCount;
    private Integer badDuration;

    // 부위별 BAD 횟수
    private Integer shoulderBadCount;
    private Integer faceBadCount;

    // 부위별 BAD 지속 시간
    private Integer shoulderBadDuration;
    private Integer faceBadDuration;

    // 점수
    private Integer score;
    // 등급
    @Enumerated(EnumType.STRING)
    private PoseGrade grade;
}
