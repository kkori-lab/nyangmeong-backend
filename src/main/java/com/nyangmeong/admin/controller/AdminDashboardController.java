package com.nyangmeong.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.admin.dto.AdminDashboardResponse;
import com.nyangmeong.admin.service.AdminDashboardService;
import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 대시보드 컨트롤러]
 * - SCR-ADM-001 (FN-ADM-001-01·02·03) : 관리자 권한은 SecurityConfig가 /api/admin/** 로 검사
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "관리자 대시보드 운영 현황·처리 대기")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.get()));
    }
}
