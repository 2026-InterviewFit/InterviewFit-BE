package com.example.interviewfitbe.global.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {

    private AuthUtils() {}

    /**
     * SecurityContext에서 인증된 사용자 ID를 꺼낸다.
     *
     * JwtAuthenticationFilter에서 UsernamePasswordAuthenticationToken의
     * principal에 userId(Long)를 넣어두었다.
     *
     * @throws IllegalStateException 인증 정보가 없거나 형식이 잘못된 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 요청입니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long) {
            return (Long) principal;
        }

        throw new IllegalStateException(
                "인증 정보가 잘못되었습니다. principal=" + principal
        );
    }

    /**
     * 필요한 경우 null-safe로 얻고 싶을 때 사용.
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}