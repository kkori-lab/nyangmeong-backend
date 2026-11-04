package com.nyangmeong.health.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.HealthIndicatorResponse;
import com.nyangmeong.health.service.HealthIndicatorService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [메인 건강 지표 컨트롤러]
 * - SCR-HLT-002 메인 건강 지표 조회 (FN-MAN-005·006, docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/health-indicators")
public class HealthIndicatorController {

    private final HealthIndicatorService healthIndicatorService;

    @Operation(summary = "메인 건강 지표 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<HealthIndicatorResponse>> get(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn) {
        return ResponseEntity.ok(ApiResponse.success(healthIndicatorService.get(me.id(), petSn)));
    }
}
