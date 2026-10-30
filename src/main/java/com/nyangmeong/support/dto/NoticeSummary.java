package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [공지사항 목록 항목]
 * - GET /api/notices (FN-CSC-001-01)
 */
@Data
public class NoticeSummary {
    private Long ntcMttrSn;
    private String ntcMttrTtl;
    private boolean upendFixYn;
    private LocalDateTime regDt;
}
