package com.nyangmeong.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.support.dto.AdminNoticeCreateRequest;
import com.nyangmeong.support.dto.AdminNoticeImageResponse;
import com.nyangmeong.support.dto.AdminNoticeResponse;
import com.nyangmeong.support.dto.AdminNoticeSearchCond;
import com.nyangmeong.support.dto.AdminNoticeSummary;
import com.nyangmeong.support.dto.AdminNoticeTargetCountResponse;
import com.nyangmeong.support.dto.AdminNoticeUpdateRequest;
import com.nyangmeong.support.service.AdminNoticeService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 공지사항 컨트롤러]
 * - SCR-ADM-040 (FN-ADM-040-01~05)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @Operation(summary = "공지 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminNoticeSummary>>> search(AdminNoticeSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminNoticeService.search(cond, pq)));
    }

    @Operation(summary = "공지 알림 대상 수")
    @GetMapping("/notification-target-count")
    public ResponseEntity<ApiResponse<AdminNoticeTargetCountResponse>> getNotificationTargetCount() {
        return ResponseEntity.ok(ApiResponse.success(adminNoticeService.getNotificationTargetCount()));
    }

    @Operation(summary = "공지 상세")
    @GetMapping("/{ntcMttrSn}")
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> get(@PathVariable Long ntcMttrSn) {
        return ResponseEntity.ok(ApiResponse.success(adminNoticeService.get(ntcMttrSn)));
    }

    @Operation(summary = "공지 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminNoticeCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminNoticeService.create(me.id(), req)));
    }

    @Operation(summary = "공지 수정·비노출")
    @PatchMapping("/{ntcMttrSn}")
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long ntcMttrSn, @Valid @RequestBody AdminNoticeUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminNoticeService.update(me.id(), ntcMttrSn, req)));
    }

    @Operation(summary = "공지 본문 이미지 업로드")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminNoticeImageResponse>> uploadImage(@AuthenticationPrincipal LoginMember me,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminNoticeService.uploadImage(me.id(), image)));
    }
}
