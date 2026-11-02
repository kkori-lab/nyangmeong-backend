package com.nyangmeong.pet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.pet.dto.AdminPointLedgerSummary;
import com.nyangmeong.pet.service.PointService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 포인트 컨트롤러]
 * - SCR-ADM-011 (FN-ADM-011-04)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members/{mbrSn}/point-ledgers")
public class AdminPointController {

    private final PointService pointService;

    @Operation(summary = "회원 포인트 원장")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPointLedgerSummary>>> searchLedgersForAdmin(
            @PathVariable Long mbrSn, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(pointService.searchLedgersForAdmin(mbrSn, pq)));
    }
}
