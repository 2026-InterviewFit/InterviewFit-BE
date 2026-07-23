package com.example.interviewfitbe.domain.user.client;

import com.example.interviewfitbe.domain.user.dto.KakaoUserInfoResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class KakaoApiClient {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient = WebClient.create();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String clientSecret;

    /**
     * 인가 코드 -> AccessToken
     */
    public String getAccessToken(String code) {

        System.out.println("========== 카카오 토큰 요청 ==========");
        System.out.println("clientId = " + clientId);
        System.out.println("redirectUri = " + redirectUri);
        System.out.println("code = " + code);
        System.out.println("clientSecret = " +
                (clientSecret == null || clientSecret.isBlank() ? "(없음)" : "(있음)"));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }

        try {

            KakaoTokenResponse response = webClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();

            System.out.println("AccessToken 발급 성공");

            if (response == null || response.getAccessToken() == null) {
                throw new RuntimeException("AccessToken이 null입니다.");
            }

            return response.getAccessToken();

        } catch (WebClientResponseException e) {

            System.out.println("========== 카카오 응답 ==========");
            System.out.println(e.getResponseBodyAsString());

            throw e;
        }
    }

    /**
     * AccessToken -> 사용자 정보 조회
     */
    public KakaoUserInfoResponse getUserInfo(String accessToken) {

        return webClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .headers(header -> header.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();
    }

    @Getter
    @Setter
    public static class KakaoTokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;
    }
}