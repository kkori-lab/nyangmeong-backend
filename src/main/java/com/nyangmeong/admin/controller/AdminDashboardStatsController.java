package com.nyangmeong.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.admin.dto.AdminDashboardStatsResponse;
import com.nyangmeong.admin.dto.AdminDashboardStatsSearchCond;
import com.nyangmeong.admin.service.AdminDashboardStatsService;
import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 대시보드 이용 통계 컨트롤러]
 * - SCR-ADM-001 (FN-ADM-001-04·05) : 관리자 권한은 SecurityConfig가 /api/admin/** 로 검사
 * - 운영 현황·처리 대기는 AdminDashboardController
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardStatsController {

    private final AdminDashboardStatsService adminDashboardStatsService;

    @Operation(summary = "관리자 대시보드 이용 통계")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> get(AdminDashboardStatsSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardStatsService.get(cond)));
    }
}
