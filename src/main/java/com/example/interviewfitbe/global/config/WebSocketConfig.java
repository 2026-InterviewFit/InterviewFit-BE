package com.example.interviewfitbe.global.config;

import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern ROOM_DESTINATION = Pattern.compile(
            "^/(?:topic/room|app/room/[^/]+)/(\\d+)$"
    );

    private final GroupParticipantRepository groupParticipantRepository;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

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
                .setAllowedOrigins(
                        Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isBlank())
                                .toArray(String[]::new)
                )
                .withSockJS(); // 웹소켓을 지원하지 않는 브라우저를 위한 SockJS fallback 활성화
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                if (command == null) {
                    return message;
                }

                Authentication authentication = authenticated(accessor);

                if (command == StompCommand.CONNECT && authentication == null) {
                    throw new IllegalStateException("WebSocket 연결에 로그인이 필요합니다.");
                }

                if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
                    if (authentication == null) {
                        throw new IllegalStateException("인증되지 않은 WebSocket 요청입니다.");
                    }
                    validateRoomMembership(accessor.getDestination(), (Long) authentication.getPrincipal());
                }

                return message;
            }
        });
    }

    private Authentication authenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long) {
            return authentication;
        }
        return null;
    }

    private void validateRoomMembership(String destination, Long userId) {
        if (destination == null) {
            return;
        }
        Matcher matcher = ROOM_DESTINATION.matcher(destination);
        if (matcher.matches()) {
            Long roomId = Long.valueOf(matcher.group(1));
            if (!groupParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
                throw new IllegalStateException("해당 면접 방의 참가자가 아닙니다.");
            }
        }
    }
}
