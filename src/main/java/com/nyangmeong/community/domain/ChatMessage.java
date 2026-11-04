package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [채팅 메시지 VO]
 * - CHAT_MSG 1:1
 */
@Data
public class ChatMessage {
    private Long chatMsgSn;
    private Long otoChatRoomSn;
    private Long mbrSn;
    private String msgTypeCd;
    private Integer msgSeq;
    private String msgCnEnc;
    private String idmptDmndId;
    private String shrnAddrEnc;
    private LocalDateTime regDt;
}
