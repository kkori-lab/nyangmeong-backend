package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 신고 처리 이력 항목]
 * - 처리자 없음은 시스템 처리
 */
@Data
public class AdminReportStatusHistorySummary {
    private Long dclrSttsHstrySn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private Long prcsMbrSn;
    private String prcsMbrNcnm;
    private LocalDateTime regDt;
}
