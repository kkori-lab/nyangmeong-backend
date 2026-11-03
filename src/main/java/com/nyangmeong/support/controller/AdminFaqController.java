package com.nyangmeong.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.support.dto.AdminFaqCreateRequest;
import com.nyangmeong.support.dto.AdminFaqResponse;
import com.nyangmeong.support.dto.AdminFaqSearchCond;
import com.nyangmeong.support.dto.AdminFaqSummary;
import com.nyangmeong.support.dto.AdminFaqUpdateRequest;
import com.nyangmeong.support.service.AdminFaqService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 FAQ 컨트롤러]
 * - SCR-ADM-041 (FN-ADM-041-01~04)
 * - 카테고리 목록은 GET /api/common-codes?prntComCd=CSCG01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/faqs")
public class AdminFaqController {

    private final AdminFaqService adminFaqService;

    @Operation(summary = "FAQ 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminFaqSummary>>> search(AdminFaqSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminFaqService.search(cond, pq)));
    }

    @Operation(summary = "FAQ 상세")
    @GetMapping("/{faqSn}")
    public ResponseEntity<ApiResponse<AdminFaqResponse>> get(@PathVariable Long faqSn) {
        return ResponseEntity.ok(ApiResponse.success(adminFaqService.get(faqSn)));
    }

    @Operation(summary = "FAQ 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminFaqResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminFaqCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminFaqService.create(me.id(), req)));
    }

    @Operation(summary = "FAQ 수정·비노출")
    @PatchMapping("/{faqSn}")
    public ResponseEntity<ApiResponse<AdminFaqResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long faqSn, @Valid @RequestBody AdminFaqUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminFaqService.update(me.id(), faqSn, req)));
    }
}
