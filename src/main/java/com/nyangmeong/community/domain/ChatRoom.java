package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [일대일 채팅방 VO]
 * - OTO_CHAT_ROOM 1:1
 */
@Data
public class ChatRoom {
    private Long otoChatRoomSn;
    private Long pstSn;
    private Long ctptMbrSn;
    private String chatRoomSttsCd;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
