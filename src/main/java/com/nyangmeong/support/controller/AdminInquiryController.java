package com.nyangmeong.support.controller;

import org.springframework.http.CacheControl;
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
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.support.dto.AdminCharacterErrorRequest;
import com.nyangmeong.support.dto.AdminInquiryAnswerRequest;
import com.nyangmeong.support.dto.AdminInquiryResponse;
import com.nyangmeong.support.dto.AdminInquirySearchCond;
import com.nyangmeong.support.dto.AdminInquirySummary;
import com.nyangmeong.support.service.AdminInquiryService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 1:1 문의 컨트롤러]
 * - SCR-ADM-042 (FN-ADM-042-01~08)
 * - 유형·상태 선택지는 GET /api/common-codes?prntComCd=CSCG02·CSCG03
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @Operation(summary = "1:1 문의 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminInquirySummary>>> search(AdminInquirySearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminInquiryService.search(cond, pq)));
    }

    @Operation(summary = "1:1 문의 상세")
    @GetMapping("/{otoInqrySn}")
    public ResponseEntity<ApiResponse<AdminInquiryResponse>> get(@PathVariable Long otoInqrySn) {
        return ResponseEntity.ok(ApiResponse.success(adminInquiryService.get(otoInqrySn)));
    }

    @Operation(summary = "1:1 문의 첨부 이미지")
    @GetMapping("/{otoInqrySn}/images/{atchFileSn}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long otoInqrySn, @PathVariable Long atchFileSn) {
        StoredFile image = adminInquiryService.getImage(otoInqrySn, atchFileSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }

    @Operation(summary = "1:1 문의 답변 등록")
    @PostMapping("/{otoInqrySn}/answer")
    public ResponseEntity<ApiResponse<AdminInquiryResponse>> answer(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoInqrySn, @Valid @RequestBody AdminInquiryAnswerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminInquiryService.answer(me.id(), otoInqrySn, req)));
    }

    @Operation(summary = "1:1 문의 답변 수정")
    @PatchMapping("/{otoInqrySn}/answer")
    public ResponseEntity<ApiResponse<AdminInquiryResponse>> updateAnswer(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoInqrySn, @Valid @RequestBody AdminInquiryAnswerRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminInquiryService.updateAnswer(me.id(), otoInqrySn, req)));
    }

    @Operation(summary = "캐릭터 오류 처리")
    @PostMapping("/{otoInqrySn}/character-error")
    public ResponseEntity<ApiResponse<AdminInquiryResponse>> processCharacterError(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long otoInqrySn,
            @Valid @RequestBody AdminCharacterErrorRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(adminInquiryService.processCharacterError(me.id(), otoInqrySn, req)));
    }
}
