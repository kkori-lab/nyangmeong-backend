package com.nyangmeong.health.dto;

import java.util.List;

/**
 * [건강기록 대시보드 항목 응답]
 * - savedYn false 면 저장한 적 없는 회원 : 프론트가 튜토리얼을 연다
 */
public record HealthDashboardWidgetResponse(
        boolean savedYn,
        List<HealthDashboardWidgetSummary> widgets) {
}
