package com.nyangmeong.pet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.AdminPointAdjustRequest;
import com.nyangmeong.pet.dto.AdminPointLedgerRow;
import com.nyangmeong.pet.dto.AdminPointLedgerSummary;
import com.nyangmeong.pet.dto.AdminPointSearchCond;
import com.nyangmeong.pet.service.AdminPointService;
import com.nyangmeong.pet.service.PointService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 포인트 컨트롤러]
 * - SCR-ADM-011 (FN-ADM-011-04) 회원 한 명 원장
 * - SCR-ADM-072 (FN-ADM-072-01·02) 전체 원장·수동 조정
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminPointController {

    private final PointService pointService;
    private final AdminPointService adminPointService;

    @Operation(summary = "회원 포인트 원장")
    @GetMapping("/members/{mbrSn}/point-ledgers")
    public ResponseEntity<ApiResponse<PageResponse<AdminPointLedgerSummary>>> searchLedgersForAdmin(
            @PathVariable Long mbrSn, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(pointService.searchLedgersForAdmin(mbrSn, pq)));
    }

    @Operation(summary = "포인트 원장 전체")
    @GetMapping("/points")
    public ResponseEntity<ApiResponse<PageResponse<AdminPointLedgerRow>>> search(AdminPointSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminPointService.search(cond, pq)));
    }

    @Operation(summary = "포인트 수동 조정")
    @PostMapping("/points/adjustments")
    public ResponseEntity<ApiResponse<AdminPointLedgerRow>> adjust(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminPointAdjustRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminPointService.adjust(me.id(), req)));
    }
}
