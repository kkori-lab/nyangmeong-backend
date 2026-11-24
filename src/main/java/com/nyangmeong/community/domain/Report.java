package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [신고 VO]
 * - DCLR 1:1
 */
@Data
public class Report {
    private Long dclrSn;
    private Long cmntSn;
    private Long mbrSn;
    private Long prcsMbrSn;
    private Long pstSn;
    /** 채팅 상대 신고의 채팅방 */
    private Long otoChatRoomSn;
    /** 채팅 상대 신고의 대상 회원 */
    private Long trgtMbrSn;
    private String dclrRsnCd;
    private String dclrPrcsSttsCd;
    private String dclrActnCd;
    private String dclrDtlCn;
    private LocalDateTime prcsDt;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
