package com.nyangmeong.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [AI 건강분석 리포트 분석 입력]
 * - 기준일 포함 14일 일상기록과 가장 최근 완료 검진의 참고 범위 밖 수치
 */
public record HealthReportInputResponse(String petNm, String petSeCd, String vrtyNm, Integer ageMm, boolean nutrYn,
        BigDecimal goalBdwg, List<HealthReportDailySummary> weights, List<HealthReportDailySummary> meals,
        List<HealthReportDailySummary> waters, List<HealthReportDailySummary> walks,
        List<HealthReportStoolSummary> stools, LocalDate latestCkupYmd,
        List<HealthReportInspectionSummary> inspOutOfRange) {
}
