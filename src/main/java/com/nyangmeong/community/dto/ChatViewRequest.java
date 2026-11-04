package com.nyangmeong.community.dto;

/**
 * [채팅방 보는 중 알림 요청]
 * - STOMP /app/chat/view, otoChatRoomSn 이 null 이면 보는 방 없음
 */
public record ChatViewRequest(Long otoChatRoomSn) {
}
