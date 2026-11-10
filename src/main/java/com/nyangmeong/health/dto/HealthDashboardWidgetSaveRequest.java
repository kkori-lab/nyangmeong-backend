package com.nyangmeong.health.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [건강기록 대시보드 항목 저장 요청]
 * - PUT /api/members/me/dashboard-widgets
 * - 13개 코드를 보일 순서대로 한 번씩. 모르는·중복·빠진 코드는 서비스가 거절
 */
public record HealthDashboardWidgetSaveRequest(
        @NotEmpty(message = "항목 목록을 입력해 주세요.") List<@NotNull(message = "항목을 입력해 주세요.") @Valid Widget> widgets) {

    /** 항목 1개의 코드와 표시여부 */
    public record Widget(
            @NotBlank(message = "항목 코드를 입력해 주세요.") @Size(max = 8, message = "항목 코드는 8자 이하입니다.") String dshbdWdgtCd,
            @NotNull(message = "표시여부를 입력해 주세요.") Boolean indctYn) {
    }
}
