package com.example.interviewfitbe.domain.user.controller;

import com.example.interviewfitbe.domain.user.client.KakaoApiClient;
import com.example.interviewfitbe.domain.user.dto.KakaoUserInfoResponse;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.service.UserService;
import com.example.interviewfitbe.global.jwt.JwtTokenProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KakaoApiClient kakaoApiClient;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/kakao-login")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request
    ) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("카카오 인가 코드가 없습니다.");
        }

        System.out.println("======================================");
        System.out.println("KAKAO LOGIN API START");
        System.out.println("======================================");

        try {
            String kakaoAccessToken =
                    kakaoApiClient.getAccessToken(request.getCode());

            KakaoUserInfoResponse kakaoUserInfo =
                    kakaoApiClient.getUserInfo(kakaoAccessToken);

            UserEntity user =
                    userService.processKakaoLogin(
                            kakaoUserInfo.getId(),
                            kakaoUserInfo.getEmail(),
                            kakaoUserInfo.getNickname()
                    );

            String jwt = jwtTokenProvider.createToken(user.getId());

            System.out.println("카카오 로그인 성공. userId=" + user.getId());

            return ResponseEntity.ok(
                    KakaoLoginResponse.from(jwt, user)
            );

        } catch (WebClientResponseException e) {
            System.out.println("카카오 로그인 실패. status=" + e.getStatusCode());
            throw e;

        } catch (Exception e) {
            System.out.println("카카오 로그인 실패. message=" + e.getMessage());
            throw e;
        }
    }

    @Getter
    public static class KakaoLoginRequest {
        private String code;
    }

    @Getter
    @RequiredArgsConstructor
    public static class KakaoLoginResponse {
        private final String token;
        private final UserResponse user;

        public static KakaoLoginResponse from(String token, UserEntity user) {
            return new KakaoLoginResponse(
                    token,
                    UserResponse.from(user)
            );
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class UserResponse {
        private final Long id;
        private final Long kakaoId;
        private final String email;
        private final String name;
        private final LocalDateTime createdAt;

        public static UserResponse from(UserEntity user) {
            return new UserResponse(
                    user.getId(),
                    user.getKakaoId(),
                    user.getEmail(),
                    user.getName(),
                    user.getCreatedAt()
            );
        }
    }
}