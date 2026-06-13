package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 면접을 본 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 면접을 본 직무
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRoleEntity jobRole;

    // 특정 사용자의 총 면접 횟수
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 면접 질문
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = true)
    private QuestionEntity question;

    // 자세 분석 결과
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pose_analysis_id", nullable = true)
    private PoseAnalysisResultEntity poseAnalysis;
}