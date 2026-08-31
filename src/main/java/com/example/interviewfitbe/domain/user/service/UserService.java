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
     * 카카오 로그인 / 회원가입 처리.
     *
     * 카카오 고유 ID를 기준으로 이미 가입된 유저가 있으면 기존 유저를 반환하고,
     * 없으면 새로 가입시킨다.
     */
    @Transactional
    public UserEntity processKakaoLogin(Long kakaoId, String email, String name) {
        if (kakaoId == null) {
            throw new IllegalArgumentException("kakaoId는 필수입니다.");
        }

        return userRepository.findByKakaoId(kakaoId)
                .map(existingUser -> {
                    existingUser.updateKakaoProfile(email, name);
                    return existingUser;
                })
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .kakaoId(kakaoId)
                            .email(email)
                            .name(resolveName(name))
                            .build();

                    return userRepository.save(newUser);
                });
    }

    @Transactional(readOnly = true)
    public UserEntity getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId)
                );
    }

    private String resolveName(String name) {
        if (name == null || name.isBlank()) {
            return "카카오사용자";
        }

        return name;
    }
}