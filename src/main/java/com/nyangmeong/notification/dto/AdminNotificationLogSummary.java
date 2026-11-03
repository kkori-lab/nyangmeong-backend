package com.nyangmeong.notification.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 알림 발송 이력 목록 행]
 * - FN-ADM-091-01 : 알림 본문은 보여 주지 않는다
 */
@Data
public class AdminNotificationLogSummary {
    private Long notiSndngLogSn;
    private Long mbrSn;
    /** 수신 회원 닉네임 : 회원 행이 없으면 null */
    private String mbrNcnm;
    private String notiTypeCd;
    private String notiChnlCd;
    private String sndngRsltCd;
    private Integer rtryNmtm;
    private String failRsn;
    private LocalDateTime sndngDt;
}
