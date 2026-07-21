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

        System.out.println("======================================");
        System.out.println("🔥🔥🔥 KAKAO LOGIN API START 🔥🔥🔥");
        System.out.println("======================================");

        try {

            System.out.println("1️⃣ 프론트에서 받은 인가 코드");
            System.out.println(request.getCode());

            //----------------------------------
            // AccessToken 발급
            //----------------------------------
            System.out.println("2️⃣ 카카오 AccessToken 요청");

            String kakaoAccessToken =
                    kakaoApiClient.getAccessToken(request.getCode());

            System.out.println("AccessToken 발급 성공");
            System.out.println(kakaoAccessToken);

            //----------------------------------
            // 사용자 조회
            //----------------------------------
            System.out.println("3️⃣ 카카오 사용자 조회");

            KakaoUserInfoResponse kakaoUserInfo =
                    kakaoApiClient.getUserInfo(kakaoAccessToken);

            System.out.println("카카오 사용자 조회 성공");

            System.out.println("id = " + kakaoUserInfo.getId());
            System.out.println("email = " + kakaoUserInfo.getEmail());
            System.out.println("nickname = " + kakaoUserInfo.getNickname());

            //----------------------------------
            // DB 회원조회
            //----------------------------------
            System.out.println("4️⃣ DB 회원 조회/회원가입");

            UserEntity user =
                    userService.processKakaoLogin(
                            kakaoUserInfo.getId(),
                            kakaoUserInfo.getEmail(),
                            kakaoUserInfo.getNickname()
                    );

            System.out.println("DB 처리 완료");

            System.out.println("userId = " + user.getId());
            System.out.println("email = " + user.getEmail());

            //----------------------------------
            // JWT 생성
            //----------------------------------
            System.out.println("5️⃣ JWT 생성");

            String jwt =
                    jwtTokenProvider.createToken(user.getId());

            System.out.println("JWT 생성 성공");
            System.out.println(jwt);

            System.out.println("======================================");
            System.out.println("🎉 카카오 로그인 성공");
            System.out.println("======================================");

            return ResponseEntity.ok(
                    new KakaoLoginResponse(jwt, user)
            );

        } catch (Exception e) {

            System.out.println("======================================");
            System.out.println("❌ 카카오 로그인 실패");
            System.out.println("======================================");

            e.printStackTrace();

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
        private final UserEntity user;
    }
}