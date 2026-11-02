package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 게시글 상태 이력 항목]
 * - 처리자가 없으면 시스템 처리 (신고 누적 자동 블라인드)
 */
@Data
public class AdminPostStatusHistorySummary {
    private Long pstSttsHstrySn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private Long prcsMbrSn;
    private String prcsMbrNcnm;
    private LocalDateTime regDt;
}
