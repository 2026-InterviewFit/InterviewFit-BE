package com.example.interviewfitbe.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId; // ★ 카카오 고유 회원 번호 (필수)

    @Column(nullable = true)
    private String email; // 카카오 계정 이메일 (선택 동의일 수 있으므로 nullable)

    @Column(nullable = false)
    private String name; // 카카오 닉네임 또는 이름

    @CreationTimestamp
    private LocalDateTime createdAt;
}