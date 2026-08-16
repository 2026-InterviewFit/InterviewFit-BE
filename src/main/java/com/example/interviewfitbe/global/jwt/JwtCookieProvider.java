package com.example.interviewfitbe.global.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtCookieProvider {

    /**
     * 브라우저에 저장될 쿠키 이름
     */
    public static final String ACCESS_TOKEN_COOKIE_NAME =
            "interviewfit_access_token";

    public static final String REFRESH_TOKEN_COOKIE_NAME =
            "interviewfit_refresh_token";


    private final boolean secure;

    private final String sameSite;

    private final long accessTokenValidityInMs;

    private final long refreshTokenValidityInMs;


    public JwtCookieProvider(
            @Value("${jwt.cookie.secure:false}")
            boolean secure,

            @Value("${jwt.cookie.same-site:Lax}")
            String sameSite,

            @Value("${jwt.access-expiration-ms}")
            long accessTokenValidityInMs,

            @Value("${jwt.refresh-expiration-ms}")
            long refreshTokenValidityInMs
    ) {
        this.secure =
                secure;

        this.sameSite =
                sameSite;

        this.accessTokenValidityInMs =
                accessTokenValidityInMs;

        this.refreshTokenValidityInMs =
                refreshTokenValidityInMs;
    }


    /**
     * Access Token HttpOnly Cookie 생성
     */
    public ResponseCookie createAccessTokenCookie(
            String accessToken
    ) {
        return ResponseCookie
                .from(
                        ACCESS_TOKEN_COOKIE_NAME,
                        accessToken
                )

                /*
                 * JavaScript에서 document.cookie로
                 * JWT를 읽을 수 없도록 한다.
                 */
                .httpOnly(
                        true
                )

                /*
                 * 개발:
                 * http://localhost
                 * → false
                 *
                 * 배포:
                 * https
                 * → true
                 */
                .secure(
                        secure
                )

                /*
                 * 모든 API 요청에서 사용
                 */
                .path(
                        "/"
                )

                /*
                 * localhost 개발에서는 Lax
                 */
                .sameSite(
                        sameSite
                )

                .maxAge(
                        Duration.ofMillis(
                                accessTokenValidityInMs
                        )
                )

                .build();
    }


    /**
     * Refresh Token HttpOnly Cookie 생성
     */
    public ResponseCookie createRefreshTokenCookie(
            String refreshToken
    ) {
        return ResponseCookie
                .from(
                        REFRESH_TOKEN_COOKIE_NAME,
                        refreshToken
                )

                .httpOnly(
                        true
                )

                .secure(
                        secure
                )

                .path(
                        "/"
                )

                .sameSite(
                        sameSite
                )

                .maxAge(
                        Duration.ofMillis(
                                refreshTokenValidityInMs
                        )
                )

                .build();
    }


    /**
     * 로그아웃 시 Access Token Cookie 삭제
     */
    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie
                .from(
                        ACCESS_TOKEN_COOKIE_NAME,
                        ""
                )

                .httpOnly(
                        true
                )

                .secure(
                        secure
                )

                .path(
                        "/"
                )

                .sameSite(
                        sameSite
                )

                /*
                 * Max-Age=0
                 * → 브라우저에서 즉시 삭제
                 */
                .maxAge(
                        Duration.ZERO
                )

                .build();
    }


    /**
     * 로그아웃 시 Refresh Token Cookie 삭제
     */
    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie
                .from(
                        REFRESH_TOKEN_COOKIE_NAME,
                        ""
                )

                .httpOnly(
                        true
                )

                .secure(
                        secure
                )

                .path(
                        "/"
                )

                .sameSite(
                        sameSite
                )

                .maxAge(
                        Duration.ZERO
                )

                .build();
    }
}