package com.example.interviewfitbe.domain.user.service;

import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 카카오 로그인 / 회원가입 처리
     * 카카오 고유 ID를 기준으로 이미 가입된 유저가 있으면 반환하고, 없으면 새로 가입시킵니다.
     */
    @Transactional
    public UserEntity processKakaoLogin(Long kakaoId, String email, String name) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    // 신규 회원가입
                    UserEntity newUser = UserEntity.builder()
                            .kakaoId(kakaoId)
                            .email(email != null ? email : "kakao_" + kakaoId + "@placeholder.com") // 이메일이 없을 경우 대비
                            .name(name != null ? name : "카카오사용자")
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * ID로 사용자 조회
     */
    @Transactional(readOnly = true)
    public UserEntity getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));
    }
}