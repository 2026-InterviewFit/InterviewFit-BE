package com.example.interviewfitbe.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 카카오 https://kapi.kakao.com/v2/user/me 응답 파싱용 DTO
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoResponse {

    private Long id; // 카카오 고유 회원 번호 (위조 불가, 카카오 서버가 검증)

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String nickname;
    }

    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.getEmail() : null;
    }

    public String getNickname() {
        return kakaoAccount != null && kakaoAccount.getProfile() != null
                ? kakaoAccount.getProfile().getNickname()
                : null;
    }
}