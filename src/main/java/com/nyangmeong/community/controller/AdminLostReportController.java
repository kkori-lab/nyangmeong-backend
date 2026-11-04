package com.nyangmeong.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.AdminLostReportResponse;
import com.nyangmeong.community.dto.AdminLostReportSearchCond;
import com.nyangmeong.community.dto.AdminLostReportSummary;
import com.nyangmeong.community.service.AdminLostReportService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 실종제보 컨트롤러]
 * - SCR-ADM-021 (FN-ADM-021-01·02), 강제 만료·기간 조정 API 없음 (FN-ADM-021-03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/lost-reports")
public class AdminLostReportController {

    private final AdminLostReportService adminLostReportService;

    @Operation(summary = "실종제보 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminLostReportSummary>>> search(AdminLostReportSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminLostReportService.search(cond, pq)));
    }

    @Operation(summary = "실종제보 상세")
    @GetMapping("/{lostTpofSn}")
    public ResponseEntity<ApiResponse<AdminLostReportResponse>> get(@PathVariable Long lostTpofSn) {
        return ResponseEntity.ok(ApiResponse.success(adminLostReportService.get(lostTpofSn)));
    }
}
