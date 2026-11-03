package com.nyangmeong.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.dto.AdminReportAutoBlindSummary;
import com.nyangmeong.community.dto.AdminReportCompleteRequest;
import com.nyangmeong.community.dto.AdminReportDetailResponse;
import com.nyangmeong.community.dto.AdminReportRejectRequest;
import com.nyangmeong.community.dto.AdminReportSearchCond;
import com.nyangmeong.community.dto.AdminReportSummary;
import com.nyangmeong.community.service.AdminReportService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 신고 컨트롤러]
 * - SCR-ADM-030 (FN-ADM-030-01~07)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "신고 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReportSummary>>> search(AdminReportSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminReportService.search(cond, pq)));
    }

    @Operation(summary = "자동 블라인드 대상 목록")
    @GetMapping("/auto-blind")
    public ResponseEntity<ApiResponse<PageResponse<AdminReportAutoBlindSummary>>> searchAutoBlind(PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminReportService.searchAutoBlind(pq)));
    }

    @Operation(summary = "신고 상세")
    @GetMapping("/{dclrSn}")
    public ResponseEntity<ApiResponse<AdminReportDetailResponse>> get(@PathVariable Long dclrSn) {
        return ResponseEntity.ok(ApiResponse.success(adminReportService.get(dclrSn)));
    }

    @Operation(summary = "신고 처리 완료")
    @PostMapping("/{dclrSn}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long dclrSn, @Valid @RequestBody AdminReportCompleteRequest req) {
        adminReportService.complete(me.id(), dclrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "신고 반려")
    @PostMapping("/{dclrSn}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long dclrSn, @Valid @RequestBody AdminReportRejectRequest req) {
        adminReportService.reject(me.id(), dclrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "자동 블라인드 대상 복구")
    @PostMapping("/{dclrSn}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long dclrSn, @Valid @RequestBody AdminPostStatusChangeRequest req) {
        adminReportService.restore(me.id(), dclrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
