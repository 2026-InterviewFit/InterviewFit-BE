package com.example.interviewfitbe.domain.pose.entity;

import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pose_report_id")
    private PoseReportEntity poseReport;

    // 발생 시간
    private Double timestamp;

    // 자세 이상이 발생한 신체 부위 (어깨 / 얼굴)
    @Enumerated(EnumType.STRING)
    private PoseIssueType issueType;

    // 얼마나 틀어졌는지
    private Double deviation;

    public void assignPoseReport(PoseReportEntity poseReport) {
        this.poseReport = poseReport;
    }
}
