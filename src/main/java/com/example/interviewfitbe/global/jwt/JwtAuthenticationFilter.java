package com.example.interviewfitbe.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        /*
         * Authorization 헤더가 없는 경우:
         * 공개 API일 수도 있으므로 다음 필터로 넘긴다.
         *
         * 보호된 API라면 이후 Spring Security에서 차단된다.
         */
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * 토큰이 존재하지만 만료됐거나 올바르지 않은 경우
         * 그냥 무시하지 않고 명확하게 401을 반환한다.
         */
        if (!jwtTokenProvider.validateToken(token)) {
            writeUnauthorizedResponse(response);
            return;
        }

        try {
            Long userId = jwtTokenProvider.getUserId(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token =
                authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            return null;
        }

        return token;
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.getWriter().write(
                """
                {
                  "status": 401,
                  "code": "INVALID_OR_EXPIRED_TOKEN",
                  "message": "JWT가 만료되었거나 유효하지 않습니다. 다시 로그인해주세요."
                }
                """
        );
    }
}