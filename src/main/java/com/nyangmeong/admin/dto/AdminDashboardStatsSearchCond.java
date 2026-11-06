package com.nyangmeong.admin.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 대시보드 이용 통계 기간]
 * - SCR-ADM-001 (FN-ADM-001-05) : 양끝 포함, 최대 90일, 종료일은 오늘까지
 * - 최근 7일·30일은 프론트가 날짜로 바꿔 보낸다
 */
@Data
public class AdminDashboardStatsSearchCond {
    /** 시작일자 */
    private LocalDate fromYmd;
    /** 종료일자 */
    private LocalDate toYmd;
}
