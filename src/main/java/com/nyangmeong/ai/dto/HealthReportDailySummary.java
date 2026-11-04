package com.nyangmeong.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [AI 건강분석 리포트 하루 값]
 * - 체중은 그날 마지막 기록, 식사량·음수량·산책은 그날 합계
 */
public record HealthReportDailySummary(LocalDate ymd, BigDecimal rcdNvl) {
}
