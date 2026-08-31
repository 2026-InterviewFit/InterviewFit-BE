package com.example.interviewfitbe.domain.job.entity;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
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
@Table(name = "recommended_jobs")
public class RecommendedJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id", nullable = false)
    private CoverLetterEntity coverLetter;

    // 추천 직무
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRoleEntity jobRole;

    // 추천 직무가 '기타'일 경우 직무명을 저장
    @Column(length = 100)
    private String customJobName;

    @Column(nullable = false)
    private Float score;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
