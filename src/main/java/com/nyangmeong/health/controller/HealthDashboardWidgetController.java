package com.nyangmeong.health.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.HealthDashboardWidgetResponse;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest;
import com.nyangmeong.health.service.HealthDashboardWidgetService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [건강기록 대시보드 고른 항목 컨트롤러]
 * - 고른 항목·순서와 반려동물별 기록 있는 항목 조회, 고른 항목 저장 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/dashboard-widgets")
public class HealthDashboardWidgetController {

    private final HealthDashboardWidgetService healthDashboardWidgetService;

    @Operation(summary = "건강기록 대시보드 고른 항목 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<HealthDashboardWidgetResponse>> get(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(healthDashboardWidgetService.get(me.id())));
    }

    @Operation(summary = "건강기록 대시보드 고른 항목 저장")
    @PutMapping
    public ResponseEntity<ApiResponse<HealthDashboardWidgetResponse>> save(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody HealthDashboardWidgetSaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(healthDashboardWidgetService.save(me.id(), req)));
    }
}
