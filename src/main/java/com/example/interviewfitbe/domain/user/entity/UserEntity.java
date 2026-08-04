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

    /**
     * 카카오 고유 회원 번호.
     * 카카오 서버에서 내려주는 id를 저장한다.
     */
    @Column(nullable = false, unique = true)
    private Long kakaoId;

    /**
     * 카카오 계정 이메일.
     * 사용자가 이메일 제공에 동의하지 않으면 null일 수 있다.
     */
    @Column(nullable = true)
    private String email;

    /**
     * 카카오 프로필 닉네임.
     */
    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateKakaoProfile(String email, String name) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }

        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }
}