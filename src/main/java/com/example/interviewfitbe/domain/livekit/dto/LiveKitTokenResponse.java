package com.example.interviewfitbe.domain.livekit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveKitTokenResponse {

    private final String url;
    private String token;

    public LiveKitTokenResponse(String token) {
        this(null, token);
    }

    public LiveKitTokenResponse(String url, String token) {
        this.url = url;
        this.token = token;
    }

}
