package com.example.interviewfitbe.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        String accessToken = resolveAccessToken(request);

        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenProvider.validateAccessToken(accessToken)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtTokenProvider.getUserId(accessToken);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(
            HttpServletRequest request
    ) {
        String cookieToken = resolveTokenFromCookie(request);

        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }

        return resolveTokenFromAuthorizationHeader(request);
    }

    private String resolveTokenFromCookie(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (JwtCookieProvider.ACCESS_TOKEN_COOKIE_NAME
                    .equals(cookie.getName())) {

                String token = cookie.getValue();

                if (token == null || token.isBlank()) {
                    return null;
                }

                return token.trim();
            }
        }

        return null;
    }

    private String resolveTokenFromAuthorizationHeader(
            HttpServletRequest request
    ) {
        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            return null;
        }

        return token;
    }
}