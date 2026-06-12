package com.example.interviewfitbe.domain.coverletter.entity;

import com.example.interviewfitbe.domain.user.entity.UserEntity;
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
@Table(name = "cover_letter")
public class CoverLetterEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 나중에 nullable = false로 변경 필요
    private UserEntity user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
