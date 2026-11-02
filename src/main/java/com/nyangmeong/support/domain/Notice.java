package com.nyangmeong.support.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [공지사항 VO]
 * - NTC_MTTR 1:1
 */
@Data
public class Notice {
    private Long ntcMttrSn;
    private String ntcMttrTtl;
    private String ntcMttrCn;
    private Integer inqCnt;
    private String upendFixYn;
    private String useYn;
    private LocalDateTime notiSndngDt;
    private Integer notiTrgtCnt;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
