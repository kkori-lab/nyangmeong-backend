package com.nyangmeong.community.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.nyangmeong.community.dto.ChatViewRequest;
import com.nyangmeong.community.service.ChatViewRegistry;

import lombok.RequiredArgsConstructor;

/**
 * [채팅방 보는 중 알림 컨트롤러]
 * - STOMP /app/chat/view : 연결(세션)이 지금 보는 방을 알림, otoChatRoomSn 이 null 이면 해제
 * - 상대가 그 방을 보는 중이면 거래 상태 알림을 만들지 않는다(FN-COM-004-14)
 */
@Controller
@RequiredArgsConstructor
public class ChatViewController {

    private final ChatViewRegistry chatViewRegistry;

    @MessageMapping("/chat/view")
    public void view(ChatViewRequest req, Principal principal, SimpMessageHeaderAccessor accessor) {
        chatViewRegistry.view(accessor.getSessionId(), Long.valueOf(principal.getName()),
                req == null ? null : req.otoChatRoomSn());
    }
}
