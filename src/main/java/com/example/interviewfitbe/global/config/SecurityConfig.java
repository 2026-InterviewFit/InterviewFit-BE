package com.example.interviewfitbe.global.config;

import com.example.interviewfitbe.global.jwt.JwtAuthenticationFilter;
import com.example.interviewfitbe.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeHttpRequests(auth -> auth
                        // 브라우저 preflight 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 로그인 없이 접근 가능한 API
                        .requestMatchers(
                                "/error",
                                "/api/users/kakao-login",
                                "/api/job-role/**",
                                "/webhook/**",
                                "/api/group/**",
                                "/ws-interview/**"
                        ).permitAll()

                        // 방 목록/상세/참가자 목록은 공개 조회로 유지한다.
                        .requestMatchers(HttpMethod.GET, "/api/group-interviews/**").permitAll()

                        // 생성/참여/진행 제어/LiveKit 토큰 발급은 로그인 사용자가 필요하다.
                        .requestMatchers("/api/group-interviews/**").authenticated()

                        // 중복된 테스트용 토큰 API는 삭제 전까지 인증 사용자만 접근한다.
                        .requestMatchers("/api/livekit/**").authenticated()

                        // 나머지는 JWT 필요
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://172.20.14.83:3000",
                "http://localhost:63343" // 테스트용 HTML
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
