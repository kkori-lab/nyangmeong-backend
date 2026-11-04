package com.nyangmeong.global.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.nyangmeong.global.security.stomp.MemberHandshakeHandler;
import com.nyangmeong.global.security.stomp.StompAuthChannelInterceptor;

/**
 * [설정 - 채팅 실시간(STOMP over WebSocket)]
 * - 연결 /ws : 쿠키 인증된 연결만(SecurityConfig 의 anyRequest 인증), 서버 한 대 기준 내장 브로커
 * - 구독 /user/queue/chat : 회원 번호 기준 개인 큐, 보내기 /app/chat/view : 보는 방 알림
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String APP_PREFIX = "/app";
    public static final String USER_PREFIX = "/user";

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT)
                .setHandshakeHandler(new MemberHandshakeHandler())
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(QUEUE_PREFIX);
        registry.setApplicationDestinationPrefixes(APP_PREFIX);
        registry.setUserDestinationPrefix(USER_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthChannelInterceptor());
    }
}
