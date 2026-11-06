package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 대시보드 일별 OCR 요청 수]
 * - SCR-ADM-001 (FN-ADM-001-04) : 영수증 등록 수, 출처·처리 상태 무관
 */
@Data
public class AdminDailyOcrRequestSummary {
    /** 일자 */
    private LocalDate ymd;
    /** 요청 수 */
    private long cnt;
}
