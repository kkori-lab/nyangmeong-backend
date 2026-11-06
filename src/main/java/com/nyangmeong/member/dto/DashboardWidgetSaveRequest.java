package com.nyangmeong.member.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [메인 위젯 순서 저장 요청]
 * - PUT /api/members/me/dashboard-widgets (FN-MAN-007)
 * - 위젯 코드를 보일 순서대로 전부 한 번씩 담는다. 빠지거나 겹치거나 없는 코드는 서비스가 거절한다
 */
public record DashboardWidgetSaveRequest(
        @NotNull(message = "위젯 목록을 입력해 주세요.") List<@NotBlank(message = "위젯 코드를 입력해 주세요.") @Size(max = 8, message = "위젯 코드는 8자 이하입니다.") String> dshbdWdgtCds) {
}
