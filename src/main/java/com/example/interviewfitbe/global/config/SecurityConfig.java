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

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://172.20.14.83:3000,http://localhost:63343}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * CORS
                 */
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                /*
                 * CSRF
                 */
                .csrf(csrf ->
                        csrf.disable()
                )

                /*
                 * JWT Stateless
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Spring 기본 로그인 비활성화
                 */
                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )

                .formLogin(formLogin ->
                        formLogin.disable()
                )

                /*
                 * 401 / 403
                 */
                .exceptionHandling(exception ->
                        exception
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
                 * API 접근 권한
                 */
                .authorizeHttpRequests(auth ->
                        auth

                                // CORS Preflight
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                // 로그인 관련 공개 API
                                .requestMatchers(
                                        "/api/users/kakao-login",
                                        "/api/users/refresh",
                                        "/api/users/logout"
                                )
                                .permitAll()

                                // 기타 공개 API
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
                                 * 커뮤니티 공개 조회 API
                                 * =================================
                                 *
                                 * 위키 / 우수답변 / 랭킹 조회는
                                 * 비로그인 사용자도 볼 수 있도록 GET만 공개한다.
                                 * POST / PATCH / DELETE는 아래 anyRequest() 규칙에 따라
                                 * 로그인 사용자만 접근할 수 있다.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/community/**"
                                )
                                .permitAll()

                                /*
                                 * 그룹 면접
                                 *
                                 * 참가자/세션/비교/내 정보/답변 관련 조회는
                                 * 로그인 사용자만 접근한다.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/group-interviews/*/participants",
                                        "/api/group-interviews/*/session",
                                        "/api/group-interviews/*/comparisons/**",
                                        "/api/group-interviews/me/**",
                                        "/api/group-interviews/*/answers/**"
                                )
                                .authenticated()

                                // 그룹 면접 일반 GET
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/group-interviews/**"
                                )
                                .permitAll()

                                // 그룹 면접 생성/참여/제어
                                .requestMatchers(
                                        "/api/group-interviews/**"
                                )
                                .authenticated()

                                // LiveKit
                                .requestMatchers(
                                        "/api/livekit/**"
                                )
                                .authenticated()

                                // 나머지 API
                                .anyRequest()
                                .authenticated()
                )

                /*
                 * JWT Filter
                 */
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

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