package com.example.interviewfitbe.domain.pose.entity;

import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "pose_bad_logs")
public class PoseBadLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pose_report_id")
    private PoseReportEntity poseReport;

    // 발생 시간 (초 단위 추천)
    private Double timestamp;

    // 어디 문제인지 (어깨 / 얼굴 등)
    @Enumerated(EnumType.STRING)
    private PoseIssueType issueType;

    // 얼마나 틀어졌는지
    private Double deviation;

    // 연관관계 설정 메서드
    public void assignPoseAnalysisResult(PoseReportEntity poseReport) {
        this.poseReport = poseReport;
    }
}
