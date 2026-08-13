package com.example.interviewfitbe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독(Subscribe)할 토픽 경로 설정
        config.enableSimpleBroker("/topic");

        // 클라이언트가 메시지를 보낼 때(Send) 붙일 접미사 설정
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-interview")
                // ⭐ 핵심 변경: 개발 환경(Next.js 3000포트)과 추후 배포 환경을 고려한 명시적 허용
                .setAllowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000", "*")
                .withSockJS(); // 웹소켓을 지원하지 않는 브라우저를 위한 SockJS fallback 활성화
    }
}