package com.example.interviewfitbe.global.config;

import com.example.interviewfitbe.global.jwt.JwtAuthenticationFilter;
import com.example.interviewfitbe.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;


    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http

                /*
                 * =========================================
                 * CORS
                 * =========================================
                 */
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )


                /*
                 * =========================================
                 * CSRF
                 * =========================================
                 *
                 * 현재 로컬 개발 단계에서는 비활성화.
                 *
                 * 실제 배포 전에는 Cookie 인증에 맞는
                 * CSRF 방어 전략을 추가해야 한다.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )


                /*
                 * =========================================
                 * Session
                 * =========================================
                 *
                 * JWT Stateless 인증
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                /*
                 * Spring 기본 로그인 방식 비활성화
                 */
                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )

                .formLogin(formLogin ->
                        formLogin.disable()
                )


                /*
                 * =========================================
                 * 401 / 403
                 * =========================================
                 */
                .exceptionHandling(exception ->
                        exception

                                /*
                                 * 로그인되지 않았거나
                                 * 유효한 Access Token이 없는 경우
                                 */
                                .authenticationEntryPoint(
                                        (
                                                request,
                                                response,
                                                authException
                                        ) -> {

                                            response.setStatus(401);

                                            response.setCharacterEncoding(
                                                    StandardCharsets.UTF_8.name()
                                            );

                                            response.setContentType(
                                                    MediaType.APPLICATION_JSON_VALUE
                                            );

                                            response
                                                    .getWriter()
                                                    .write(
                                                            """
                                                            {
                                                              "status": 401,
                                                              "code": "UNAUTHORIZED",
                                                              "message": "로그인이 필요합니다."
                                                            }
                                                            """
                                                    );
                                        }
                                )


                                /*
                                 * 로그인은 되어 있지만
                                 * 해당 요청에 대한 권한이 없는 경우
                                 */
                                .accessDeniedHandler(
                                        (
                                                request,
                                                response,
                                                accessDeniedException
                                        ) -> {

                                            response.setStatus(403);

                                            response.setCharacterEncoding(
                                                    StandardCharsets.UTF_8.name()
                                            );

                                            response.setContentType(
                                                    MediaType.APPLICATION_JSON_VALUE
                                            );

                                            response
                                                    .getWriter()
                                                    .write(
                                                            """
                                                            {
                                                              "status": 403,
                                                              "code": "FORBIDDEN",
                                                              "message": "해당 요청에 접근할 권한이 없습니다."
                                                            }
                                                            """
                                                    );
                                        }
                                )
                )


                /*
                 * =========================================
                 * API 접근 권한
                 * =========================================
                 */
                .authorizeHttpRequests(auth ->
                        auth

                                /*
                                 * CORS Preflight
                                 */
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()


                                /*
                                 * =================================
                                 * 카카오 로그인 관련 공개 API
                                 * =================================
                                 */

                                .requestMatchers(
                                        "/api/users/kakao-login"
                                )
                                .permitAll()


                                /*
                                 * Access Token 만료 후
                                 * Refresh Token으로 재발급
                                 */
                                .requestMatchers(
                                        "/api/users/refresh"
                                )
                                .permitAll()


                                /*
                                 * Access Token이 만료됐어도
                                 * Refresh Cookie까지 삭제 가능해야 함
                                 */
                                .requestMatchers(
                                        "/api/users/logout"
                                )
                                .permitAll()


                                /*
                                 * =================================
                                 * 기타 공개 API
                                 * =================================
                                 */
                                .requestMatchers(
                                        "/error",
                                        "/api/job-role/**",
                                        "/webhook/**",
                                        "/api/group/**",
                                        "/ws-interview/**"
                                )
                                .permitAll()


                                /*
                                 * =================================
                                 * 그룹 면접
                                 * =================================
                                 *
                                 * 목록 / 상세 / 참가자 조회는 공개
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/group-interviews/*/participants",
                                        "/api/group-interviews/*/session"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/group-interviews/**"
                                )
                                .permitAll()


                                /*
                                 * 생성 / 참가 / 진행 제어 등은
                                 * 로그인 사용자만 가능
                                 */
                                .requestMatchers(
                                        "/api/group-interviews/**"
                                )
                                .authenticated()


                                /*
                                 * LiveKit 토큰 API
                                 */
                                .requestMatchers(
                                        "/api/livekit/**"
                                )
                                .authenticated()


                                /*
                                 * =================================
                                 * 나머지 API 전부 인증 필요
                                 * =================================
                                 *
                                 * /api/users/me
                                 * /api/interview/**
                                 * /api/followup/**
                                 * /api/pt-interviews/**
                                 */
                                .anyRequest()
                                .authenticated()
                )


                /*
                 * =========================================
                 * JWT Filter
                 * =========================================
                 */
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }


    /*
     * =====================================================
     * CORS
     * =====================================================
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        /*
         * allowCredentials(true) 사용 시
         * allowedOrigins에 "*" 사용 불가
         */
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList()
        );


        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );


        configuration.setAllowedHeaders(
                List.of("*")
        );


        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );


        /*
         * HttpOnly Access / Refresh Cookie 허용
         */
        configuration.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }
}
