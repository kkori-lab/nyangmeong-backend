package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 공지 목록 항목]
 * - GET /api/admin/notices (FN-ADM-040-01)
 * - notiSndngDt : null 이면 미발송
 */
@Data
public class AdminNoticeSummary {
    private Long ntcMttrSn;
    private String ntcMttrTtl;
    private boolean upendFixYn;
    private boolean useYn;
    private Integer inqCnt;
    private LocalDateTime notiSndngDt;
    private Integer notiTrgtCnt;
    private LocalDateTime regDt;
}
