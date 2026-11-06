package com.nyangmeong.ai.dto;

import java.time.LocalDate;

/**
 * [AI 건강분석 리포트 배변 기록]
 */
public record HealthReportStoolSummary(LocalDate ymd, String crsdSttsCdNm) {
}
