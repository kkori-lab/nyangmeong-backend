package com.nyangmeong.notification.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 알림 발송 이력 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminNotificationLogSearchCond {
    /** 발송 시작일자 */
    private LocalDate bgngYmd;
    /** 발송 종료일자 : 그날 끝까지 포함 */
    private LocalDate endYmd;
    /** 알림채널공통코드(AIMG03) */
    private String notiChnlCd;
    /** 알림유형공통코드(AIMG02) */
    private String notiTypeCd;
    /** 발송결과공통코드(AIMG04) */
    private String sndngRsltCd;
}
