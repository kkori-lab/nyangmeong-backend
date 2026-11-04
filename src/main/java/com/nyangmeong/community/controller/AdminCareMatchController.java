package com.nyangmeong.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.AdminCareMatchResponse;
import com.nyangmeong.community.dto.AdminCareMatchSearchCond;
import com.nyangmeong.community.dto.AdminCareMatchSummary;
import com.nyangmeong.community.service.AdminCareMatchService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 펫돌봄 거래 컨트롤러]
 * - SCR-ADM-022 (FN-ADM-022-01·02), 채팅 내용 비노출 (FN-ADM-022-03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/care-matches")
public class AdminCareMatchController {

    private final AdminCareMatchService adminCareMatchService;

    @Operation(summary = "펫돌봄 모집글 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCareMatchSummary>>> search(AdminCareMatchSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminCareMatchService.search(cond, pq)));
    }

    @Operation(summary = "펫돌봄 매칭 상세")
    @GetMapping("/{petPrtcRcrtSn}")
    public ResponseEntity<ApiResponse<AdminCareMatchResponse>> get(@PathVariable Long petPrtcRcrtSn) {
        return ResponseEntity.ok(ApiResponse.success(adminCareMatchService.get(petPrtcRcrtSn)));
    }
}
