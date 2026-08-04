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
     * 카카오 인가 코드 -> 카카오 Access Token 발급
     */
    public String getAccessToken(String code) {
        validateKakaoConfig();

        System.out.println("========== 카카오 토큰 요청 ==========");
        System.out.println("clientId = " + mask(clientId));
        System.out.println("redirectUri = " + redirectUri);
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

            if (response == null || response.getAccessToken() == null) {
                throw new IllegalStateException("카카오 AccessToken 응답이 비어 있습니다.");
            }

            System.out.println("카카오 AccessToken 발급 성공");

            return response.getAccessToken();

        } catch (WebClientResponseException e) {
            System.out.println("========== 카카오 토큰 요청 실패 ==========");
            System.out.println("status = " + e.getStatusCode());
            System.out.println("body = " + e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * 카카오 Access Token -> 사용자 정보 조회
     */
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            KakaoUserInfoResponse response = webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .headers(header -> header.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(KakaoUserInfoResponse.class)
                    .block();

            if (response == null || response.getId() == null) {
                throw new IllegalStateException("카카오 사용자 정보 응답이 비어 있습니다.");
            }

            return response;

        } catch (WebClientResponseException e) {
            System.out.println("========== 카카오 사용자 조회 실패 ==========");
            System.out.println("status = " + e.getStatusCode());
            System.out.println("body = " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private void validateKakaoConfig() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                    "카카오 REST API 키가 설정되지 않았습니다. spring.security.oauth2.client.registration.kakao.client-id 값을 확인하세요."
            );
        }

        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException(
                    "카카오 redirect-uri가 설정되지 않았습니다."
            );
        }
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "(없음)";
        }

        if (value.length() <= 8) {
            return "****";
        }

        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
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