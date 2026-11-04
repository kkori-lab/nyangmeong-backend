package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [채팅 참여자 VO]
 * - CHAT_PRTPNT 1:1
 */
@Data
public class ChatParticipant {
    private Long chatPrtpntSn;
    private Long otoChatRoomSn;
    private Long mbrSn;
    private String ptcpSttsCd;
    private Integer lastPrslMsgSeq;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
