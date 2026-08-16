package com.example.interviewfitbe.global.config;

import com.example.interviewfitbe.global.jwt.JwtAuthenticationFilter;
import com.example.interviewfitbe.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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
import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;


    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http

                /*
                 * =========================================
                 * CORS
                 * =========================================
                 *
                 * HttpOnly Cookie 인증을 사용하므로
                 * allowCredentials(true)가 적용된
                 * CORS 설정을 사용한다.
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
                 * 현재 로컬 개발 단계에서는 비활성화한다.
                 *
                 * 쿠키 인증을 사용하는 실제 배포 환경에서는
                 * CSRF 방어 전략을 별도로 적용해야 한다.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )


                /*
                 * =========================================
                 * Session
                 * =========================================
                 *
                 * Spring Session은 사용하지 않고
                 * JWT 기반 Stateless 인증을 사용한다.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                /*
                 * Spring 기본 인증 방식 비활성화
                 */
                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )

                .formLogin(formLogin ->
                        formLogin.disable()
                )


                /*
                 * =========================================
                 * 401 / 403 응답
                 * =========================================
                 */
                .exceptionHandling(exception ->
                        exception

                                /*
                                 * 인증 정보가 없거나
                                 * 유효한 Access Token이 없는 경우
                                 */
                                .authenticationEntryPoint(
                                        (
                                                request,
                                                response,
                                                authException
                                        ) -> {

                                            response.setStatus(
                                                    401
                                            );

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
                                 * 인증은 되었지만
                                 * 해당 요청 권한이 없는 경우
                                 */
                                .accessDeniedHandler(
                                        (
                                                request,
                                                response,
                                                accessDeniedException
                                        ) -> {

                                            response.setStatus(
                                                    403
                                            );

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
                                 * 브라우저 CORS Preflight
                                 */
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()


                                /*
                                 * =================================
                                 * 인증 없이 접근 가능한 API
                                 * =================================
                                 */
                                .requestMatchers(

                                        "/error",


                                        /*
                                         * 카카오 로그인
                                         */
                                        "/api/users/kakao-login",


                                        /*
                                         * Access Token 재발급
                                         *
                                         * Access Token이 이미 만료된 상태에서
                                         * Refresh Cookie로 인증을 복구해야 하므로
                                         * permitAll이어야 한다.
                                         */
                                        "/api/users/refresh",


                                        /*
                                         * Access Token이 만료됐더라도
                                         * Refresh Cookie까지 삭제할 수 있어야 한다.
                                         */
                                        "/api/users/logout",


                                        /*
                                         * 직무 목록
                                         */
                                        "/api/job-role/**",


                                        /*
                                         * 기존 공개 API
                                         */
                                        "/webhook/**",

                                        "/api/group/**",

                                        "/api/group-interviews/**",

                                        "/ws-interview/**"
                                )
                                .permitAll()


                                /*
                                 * =================================
                                 * 나머지 API
                                 * =================================
                                 *
                                 * /api/users/me
                                 * /api/interview/**
                                 * /api/followup/**
                                 * /api/pt-interviews/**
                                 * 등은 인증 필요.
                                 */
                                .anyRequest()
                                .authenticated()
                )


                /*
                 * =========================================
                 * JWT 인증 필터
                 * =========================================
                 *
                 * JwtAuthenticationFilter에서
                 *
                 * interviewfit_access_token
                 *
                 * HttpOnly Cookie를 읽어 인증한다.
                 *
                 * 전환 기간 동안 Bearer Token fallback을
                 * 유지하고 있다면 그것도 이 필터에서 처리한다.
                 */
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }


    /**
     * =====================================================
     * CORS
     * =====================================================
     *
     * HttpOnly Cookie를 FE ↔ BE 간 요청에서
     * 주고받을 수 있도록 credentials를 허용한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        /*
         * allowCredentials(true) 사용 시
         * allowedOrigins에 "*" 사용 불가.
         *
         * FE 주소를 정확하게 명시해야 한다.
         */
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",

                        /*
                         * 현재 개발 Network 주소
                         */
                        "http://172.20.2.15:3000",

                        /*
                         * 기존 개발 Network 주소
                         */
                        "http://172.20.14.83:3000"
                )
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
                List.of(
                        "*"
                )
        );


        /*
         * 기존 Bearer Token 방식과의
         * 임시 호환을 위해 유지.
         */
        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );


        /*
         * 매우 중요:
         *
         * 브라우저가 Access / Refresh Cookie를
         * Spring 요청에 포함할 수 있도록 한다.
         */
        configuration.setAllowCredentials(
                true
        );


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }
}