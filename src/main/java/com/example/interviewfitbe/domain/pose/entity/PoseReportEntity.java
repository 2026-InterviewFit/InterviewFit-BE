package com.example.interviewfitbe.domain.pose.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "pose_reports")
public class PoseReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 한 면접에는 여러 개의 답변이 존재할 수 있고,
     * 각 답변마다 자세 분석 결과가 생성될 수 있다.
     *
     * 따라서 Interview : PoseReport = 1 : N 관계이다.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "interview_id",
            nullable = false
    )
    private InterviewEntity interview;

    private Double totalDuration;

    // 전체 BAD
    private Integer badCount;
    private Double badDuration;

    // 부위별 BAD 구간 횟수
    private Integer shoulderBadCount;
    private Integer faceBadCount;

    // 부위별 BAD 지속 시간
    private Double shoulderBadDuration;
    private Double faceBadDuration;

    // 전체 Not Detected 지속 시간
    private Double notDetectedDuration;

    // 부위별 Not Detected 지속 시간
    private Double shoulderNotDetectedDuration;
    private Double faceNotDetectedDuration;

    // BAD 비율
    private Double badRatio;

    // 점수
    private Integer score;

    // 등급
    @Enumerated(EnumType.STRING)
    private PoseGrade grade;

    @CreationTimestamp
    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}