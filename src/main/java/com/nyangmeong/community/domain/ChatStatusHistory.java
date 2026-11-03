package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [채팅방·참여자 상태 이력 VO]
 * - OTO_CHAT_ROOM_STTS_HSTRY, CHAT_PRTPNT_STTS_HSTRY 가 같은 모양, refSn 은 대상 일련번호(채팅방 또는 참여자)
 */
@Data
public class ChatStatusHistory {
    private Long refSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
