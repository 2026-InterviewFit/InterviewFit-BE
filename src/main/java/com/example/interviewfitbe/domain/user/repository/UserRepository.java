package com.example.interviewfitbe.domain.user.repository;

import com.example.interviewfitbe.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 카카오 고유 ID로 사용자 조회
    Optional<UserEntity> findByKakaoId(Long kakaoId);

    // 기존 이메일 조회 메서드 유지
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}