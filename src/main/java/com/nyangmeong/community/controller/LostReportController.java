package com.nyangmeong.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.LostReportCreateRequest;
import com.nyangmeong.community.dto.LostReportResponse;
import com.nyangmeong.community.dto.LostReportSearchCond;
import com.nyangmeong.community.dto.LostReportSummary;
import com.nyangmeong.community.dto.LostReportUpdateRequest;
import com.nyangmeong.community.service.LostReportService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [실종센터 컨트롤러]
 * - SCR-CMU-002 목록·상세·등록·수정·삭제·찾았어요·게시 기간 연장, 등록·수정은 multipart/form-data
 * - 좋아요·댓글·신고는 /api/posts, /api/reports 를 같이 쓴다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lost-reports")
public class LostReportController {

    private final LostReportService lostReportService;

    @Operation(summary = "실종 제보 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LostReportSummary>>> search(@AuthenticationPrincipal LoginMember me,
            LostReportSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(lostReportService.search(me.id(), cond, pq)));
    }

    @Operation(summary = "실종 제보 상세")
    @GetMapping("/{lostTpofSn}")
    public ResponseEntity<ApiResponse<LostReportResponse>> getReport(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long lostTpofSn) {
        return ResponseEntity.ok(ApiResponse.success(lostReportService.getReport(me.id(), lostTpofSn)));
    }

    @Operation(summary = "실종 제보 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LostReportResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute LostReportCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(lostReportService.create(me.id(), req)));
    }

    @Operation(summary = "실종 제보 수정(작성자, 실종 상태만)")
    @PatchMapping(value = "/{lostTpofSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LostReportResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long lostTpofSn, @Valid @ModelAttribute LostReportUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(lostReportService.update(me.id(), lostTpofSn, req)));
    }

    @Operation(summary = "실종 제보 삭제(작성자)")
    @DeleteMapping("/{lostTpofSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long lostTpofSn) {
        lostReportService.delete(me.id(), lostTpofSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "찾았어요 전환(작성자)")
    @PostMapping("/{lostTpofSn}/found")
    public ResponseEntity<ApiResponse<Void>> markFound(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long lostTpofSn) {
        lostReportService.markFound(me.id(), lostTpofSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "게시 기간 30일 연장(작성자)")
    @PostMapping("/{lostTpofSn}/renew")
    public ResponseEntity<ApiResponse<Void>> renew(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long lostTpofSn) {
        lostReportService.renew(me.id(), lostTpofSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
