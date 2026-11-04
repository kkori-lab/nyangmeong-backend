package com.nyangmeong.ai.dto;

import java.math.BigDecimal;

/**
 * [AI 건강분석 리포트 참고 범위 밖 검사 수치]
 */
public record HealthReportInspectionSummary(String inspArtclNm, BigDecimal rsltNvl, String unitNm,
        BigDecimal refLlmtNvl, BigDecimal refUplmtNvl) {
}
