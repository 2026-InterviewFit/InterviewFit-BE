package com.example.interviewfitbe.domain.group.entity;

import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 직무 토의(4라운드)의 경우 특정 직무와 연동, 1~3라운드 공통 질문은 null 허용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id")
    private JobRoleEntity jobRole;

    @Column(nullable = false)
    private Integer roundNumber; // 1: 자기소개, 2: 공통 역량, 3: 압박, 4: 직무 토의

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText; // 면접 질문 내용

    @Column(nullable = false)
    @Builder.Default
    private boolean selectionEnabled = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
