package com.example.interviewfitbe.domain.user.controller;

import com.example.interviewfitbe.domain.user.client.KakaoApiClient;
import com.example.interviewfitbe.domain.user.dto.KakaoUserInfoResponse;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.service.UserService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import com.example.interviewfitbe.global.jwt.JwtCookieProvider;
import com.example.interviewfitbe.global.jwt.JwtTokenProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

    private final JwtCookieProvider jwtCookieProvider;


    /**
     * =====================================================
     * 카카오 로그인
     * =====================================================
     *
     * 1. FE에서 카카오 Authorization Code 전달
     * 2. 카카오 Access Token 발급
     * 3. 카카오 사용자 정보 조회
     * 4. InterviewFit 사용자 조회 / 회원가입
     * 5. InterviewFit Access Token 생성
     * 6. InterviewFit Refresh Token 생성
     * 7. 두 JWT 모두 HttpOnly Cookie로 저장
     */
    @PostMapping("/kakao-login")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(
            @RequestBody
            KakaoLoginRequest request
    ) {

        if (
                request.getCode() == null
                        || request.getCode().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "카카오 인가 코드가 없습니다."
            );
        }


        System.out.println(
                "======================================"
        );

        System.out.println(
                "KAKAO LOGIN API START"
        );

        System.out.println(
                "======================================"
        );


        try {

            /*
             * =============================================
             * 1. 카카오 Authorization Code
             *    -> 카카오 Access Token
             * =============================================
             */
            String kakaoAccessToken =
                    kakaoApiClient.getAccessToken(
                            request.getCode()
                    );


            /*
             * =============================================
             * 2. 카카오 사용자 정보 조회
             * =============================================
             */
            KakaoUserInfoResponse kakaoUserInfo =
                    kakaoApiClient.getUserInfo(
                            kakaoAccessToken
                    );


            /*
             * =============================================
             * 3. InterviewFit 사용자 조회 / 가입
             * =============================================
             */
            UserEntity user =
                    userService.processKakaoLogin(
                            kakaoUserInfo.getId(),
                            kakaoUserInfo.getEmail(),
                            kakaoUserInfo.getNickname()
                    );


            /*
             * =============================================
             * 4. InterviewFit Access Token
             * =============================================
             */
            String accessToken =
                    jwtTokenProvider
                            .createAccessToken(
                                    user.getId()
                            );


            /*
             * =============================================
             * 5. InterviewFit Refresh Token
             * =============================================
             */
            String refreshToken =
                    jwtTokenProvider
                            .createRefreshToken(
                                    user.getId()
                            );


            /*
             * =============================================
             * 6. HttpOnly Cookie 생성
             * =============================================
             */
            ResponseCookie accessCookie =
                    jwtCookieProvider
                            .createAccessTokenCookie(
                                    accessToken
                            );


            ResponseCookie refreshCookie =
                    jwtCookieProvider
                            .createRefreshTokenCookie(
                                    refreshToken
                            );


            System.out.println(
                    "카카오 로그인 성공. userId="
                            + user.getId()
            );


            /*
             * =============================================
             * 현재는 FE 호환성을 위해
             * response body에도 accessToken을 유지한다.
             *
             * 이후 FE를 Cookie 인증으로 전환하면
             * token 필드를 제거할 예정이다.
             * =============================================
             */
            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            accessCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            refreshCookie.toString()
                    )
                    .body(
                            KakaoLoginResponse.from(
                                    accessToken,
                                    user
                            )
                    );


        } catch (
                WebClientResponseException e
        ) {

            System.out.println(
                    "카카오 로그인 실패. status="
                            + e.getStatusCode()
            );

            throw e;


        } catch (
                Exception e
        ) {

            System.out.println(
                    "카카오 로그인 실패. message="
                            + e.getMessage()
            );

            throw e;
        }
    }


    /**
     * =====================================================
     * Access Token 재발급
     * =====================================================
     *
     * Access Token이 만료됐을 때
     * HttpOnly Refresh Token Cookie를 사용한다.
     *
     * FE는 Refresh Token 값을 직접 볼 필요가 없다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(
                    name = JwtCookieProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            )
            String refreshToken
    ) {

        /*
         * Refresh Cookie 자체가 없는 경우
         */
        if (
                refreshToken == null
                        || refreshToken.isBlank()
        ) {

            return ResponseEntity
                    .status(401)
                    .body(
                            new RefreshResponse(
                                    false,
                                    "Refresh Token이 없습니다."
                            )
                    );
        }


        /*
         * Refresh Token 검증
         *
         * - 만료
         * - 서명 오류
         * - access token을 refresh token으로 사용
         *
         * 모두 false 처리
         */
        if (
                !jwtTokenProvider
                        .validateRefreshToken(
                                refreshToken
                        )
        ) {

            ResponseCookie deleteAccessCookie =
                    jwtCookieProvider
                            .deleteAccessTokenCookie();

            ResponseCookie deleteRefreshCookie =
                    jwtCookieProvider
                            .deleteRefreshTokenCookie();


            return ResponseEntity
                    .status(401)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            deleteAccessCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            deleteRefreshCookie.toString()
                    )
                    .body(
                            new RefreshResponse(
                                    false,
                                    "Refresh Token이 만료되었거나 유효하지 않습니다."
                            )
                    );
        }


        /*
         * Refresh Token에서 사용자 ID 추출
         */
        Long userId =
                jwtTokenProvider.getUserId(
                        refreshToken
                );


        /*
         * 탈퇴했거나 존재하지 않는 사용자의
         * Refresh Token 사용 방지.
         */
        userService.getUserById(
                userId
        );


        /*
         * 새로운 Access Token 발급
         */
        String newAccessToken =
                jwtTokenProvider
                        .createAccessToken(
                                userId
                        );


        ResponseCookie newAccessCookie =
                jwtCookieProvider
                        .createAccessTokenCookie(
                                newAccessToken
                        );


        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        newAccessCookie.toString()
                )
                .body(
                        new RefreshResponse(
                                true,
                                "Access Token 재발급 완료"
                        )
                );
    }


    /**
     * =====================================================
     * 로그아웃
     * =====================================================
     *
     * Access / Refresh Cookie를 모두 삭제한다.
     *
     * endpoint 자체는 permitAll로 두어서
     * Access Token이 이미 만료된 상황에서도
     * 브라우저 Cookie를 정리할 수 있게 한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {

        ResponseCookie deleteAccessCookie =
                jwtCookieProvider
                        .deleteAccessTokenCookie();


        ResponseCookie deleteRefreshCookie =
                jwtCookieProvider
                        .deleteRefreshTokenCookie();


        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteAccessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteRefreshCookie.toString()
                )
                .body(
                        new LogoutResponse(
                                true,
                                "로그아웃 완료"
                        )
                );
    }


    /**
     * =====================================================
     * 현재 로그인 사용자
     * =====================================================
     *
     * 나중에 FE가 localStorage의 user 정보도 없애고
     * 서버에서 실제 로그인 사용자 정보를 가져오기 위해 사용.
     *
     * JwtAuthenticationFilter가 HttpOnly Access Token Cookie를 읽고
     * SecurityContext에 저장한 userId를 사용한다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {

        Long userId =
                AuthUtils
                        .getCurrentUserId();


        UserEntity user =
                userService
                        .getUserById(
                                userId
                        );


        return ResponseEntity
                .ok(
                        UserResponse.from(
                                user
                        )
                );
    }


    /**
     * =====================================================
     * Request / Response DTO
     * =====================================================
     */


    /**
     * 카카오 로그인 요청
     *
     * {
     *   "code": "..."
     * }
     */
    @Getter
    public static class KakaoLoginRequest {

        private String code;
    }


    /**
     * 카카오 로그인 응답
     *
     * 현재는 기존 FE 호환성을 위해 token을 유지한다.
     *
     * Cookie 전환 완료 후에는 token 필드를
     * 제거할 예정이다.
     */
    @Getter
    @RequiredArgsConstructor
    public static class KakaoLoginResponse {

        private final String token;

        private final UserResponse user;


        public static KakaoLoginResponse from(
                String token,
                UserEntity user
        ) {

            return new KakaoLoginResponse(
                    token,
                    UserResponse.from(
                            user
                    )
            );
        }
    }


    /**
     * Access Token 재발급 결과
     */
    @Getter
    @RequiredArgsConstructor
    public static class RefreshResponse {

        private final boolean success;

        private final String message;
    }


    /**
     * 로그아웃 결과
     */
    @Getter
    @RequiredArgsConstructor
    public static class LogoutResponse {

        private final boolean success;

        private final String message;
    }


    /**
     * 사용자 정보
     */
    @Getter
    @RequiredArgsConstructor
    public static class UserResponse {

        private final Long id;

        private final Long kakaoId;

        private final String email;

        private final String name;

        private final LocalDateTime createdAt;


        public static UserResponse from(
                UserEntity user
        ) {

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
