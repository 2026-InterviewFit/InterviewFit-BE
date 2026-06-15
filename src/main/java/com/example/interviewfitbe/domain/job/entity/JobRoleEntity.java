package com.example.interviewfitbe.domain.job.entity;

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
@Table(name = "job_role")
public class JobRoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 직무 카테고리 (IT/개발, 비즈니스, 디자인)
    @Column(nullable = false)
    private String category;

    // 직무명 (백엔드, 프론트엔드, 마케팅 ...)
    @Column(nullable = false)
    private String name;
}
