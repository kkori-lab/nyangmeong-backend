package com.nyangmeong.community.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * [채팅방 보는 중 등록]
 * - STOMP 연결(세션)별로 지금 보고 있는 방을 기억 -> 회원의 연결 중 하나라도 그 방을 보면 보는 중
 * - 연결이 끊기면 등록이 지워짐, 서버 한 대 기준 메모리 보관
 */
@Component
public class ChatViewRegistry {

    private record View(Long mbrSn, Long otoChatRoomSn) {
    }

    private final Map<String, View> views = new ConcurrentHashMap<>();

    /**
     * 세션이 보는 방 등록 (null 이면 해제)
     */
    public void view(String sessionId, Long mbrSn, Long otoChatRoomSn) {
        if (otoChatRoomSn == null) {
            views.remove(sessionId);
            return;
        }
        views.put(sessionId, new View(mbrSn, otoChatRoomSn));
    }

    /**
     * 회원의 연결 중 하나라도 그 방을 보고 있는지
     */
    public boolean isViewing(Long mbrSn, Long otoChatRoomSn) {
        return views.values().stream()
                .anyMatch(view -> view.mbrSn().equals(mbrSn) && view.otoChatRoomSn().equals(otoChatRoomSn));
    }

    @EventListener
    void onDisconnect(SessionDisconnectEvent event) {
        views.remove(event.getSessionId());
    }
}
