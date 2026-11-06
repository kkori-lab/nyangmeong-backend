package com.nyangmeong.ai.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.HealthReportContextResponse;
import com.nyangmeong.ai.dto.HealthReportSaveRequest;
import com.nyangmeong.ai.service.AiHealthReportService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [AI 건강분석 리포트 내부 API 컨트롤러]
 * - /internal/ai/pets/{petSn} : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (docs/api/ai.md)
 */
@RestController
@RequestMapping("/internal/ai/pets/{petSn}")
@RequiredArgsConstructor
public class AiHealthReportController {

    private final AiHealthReportService aiHealthReportService;

    @Operation(summary = "AI 건강분석 리포트 입력·저장본 조회")
    @GetMapping("/health-report-input")
    public ResponseEntity<ApiResponse<HealthReportContextResponse>> getContext(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn, @RequestParam LocalDate baseYmd) {
        return ResponseEntity.ok(ApiResponse.success(aiHealthReportService.getContext(me.id(), petSn, baseYmd)));
    }

    @Operation(summary = "AI 건강분석 리포트 저장")
    @PutMapping("/health-report")
    public ResponseEntity<ApiResponse<Void>> save(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody HealthReportSaveRequest req) {
        aiHealthReportService.save(me.id(), petSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
