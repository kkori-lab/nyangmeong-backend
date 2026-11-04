package com.nyangmeong.ai.dto;

/**
 * [AI 건강분석 리포트 입력 조회 응답]
 * - Python AI 서버가 리포트 생성·재사용 판단에 씀 (docs/api/ai.md)
 */
public record HealthReportContextResponse(HealthReportInputResponse input, HealthReportResponse report,
        boolean rfrshPsblYn, boolean autoRgnrPsblYn) {
}
