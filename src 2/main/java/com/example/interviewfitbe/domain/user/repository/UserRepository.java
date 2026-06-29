package com.example.interviewfitbe.domain.user.repository;

import com.example.interviewfitbe.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 이메일로 사용자를 찾는 메서드 (로그인이나 중복 가입 체크 시 사용)
    Optional<UserEntity> findByEmail(String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);
}