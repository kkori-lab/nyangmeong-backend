package com.nyangmeong.community.dto;

/**
 * [채팅 안 읽은 메시지 총수 응답]
 * - 99+ 표시는 화면이 한다
 */
public record ChatUnreadCountResponse(Integer unreadCnt) {
}
