package com.nyangmeong.global.security.stomp;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.nyangmeong.global.security.domain.LoginMember;

/**
 * [WebSocket 연결 사용자 결정]
 * - 연결 요청의 access_token 쿠키를 JWT 필터가 검증한 인증 정보에서 회원 번호를 꺼냄
 * - 인증 정보가 없으면 null -> StompAuthChannelInterceptor 가 CONNECT 를 막음
 */
public class MemberHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (request.getPrincipal() instanceof Authentication authentication
                && authentication.getPrincipal() instanceof LoginMember member) {
            return StompPrincipal.of(member.id());
        }
        return null;
    }
}
