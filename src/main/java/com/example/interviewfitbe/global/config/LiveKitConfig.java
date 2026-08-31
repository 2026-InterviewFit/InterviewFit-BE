package com.example.interviewfitbe.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class LiveKitConfig {

    @Value("${livekit.api-key:${LIVEKIT_API_KEY}}")
    private String apiKey;

    @Value("${livekit.api-secret:${LIVEKIT_API_SECRET}}")
    private String apiSecret;

    @Value("${livekit.url:${LIVEKIT_URL}}")
    private String url;
}
