package com.nyangmeong.health.dto;

import java.util.List;

/**
 * [건강기록 대시보드 항목 응답]
 * - savedYn false 면 저장한 적 없는 회원 : 프론트가 튜토리얼을 연다
 * - recordedItems 는 회원의 반려동물마다 기록이 있는 항목 코드
 */
public record HealthDashboardWidgetResponse(
        boolean savedYn,
        List<HealthDashboardWidgetSummary> widgets,
        List<PetRecordedItems> recordedItems) {

    /** 반려동물 1마리의 기록 있는 항목 코드(기본 순서) */
    public record PetRecordedItems(Long petSn, List<String> dshbdWdgtCds) {
    }
}
