package com.nyangmeong.expense.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.AdminReceiptListResponse;
import com.nyangmeong.expense.dto.AdminReceiptSearchCond;
import com.nyangmeong.expense.service.AdminReceiptService;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 영수증 OCR 현황 컨트롤러]
 * - SCR-ADM-082 처리 현황 영수증 OCR 탭 (FN-ADM-082-01)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/processing/receipts")
public class AdminReceiptController {

    private final AdminReceiptService adminReceiptService;

    @Operation(summary = "영수증 OCR 현황(건수·목록)")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminReceiptListResponse>> search(AdminReceiptSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminReceiptService.search(cond, pq)));
    }

    @Operation(summary = "영수증 원본 이미지(관리자)")
    @GetMapping("/{recptSn}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long recptSn) {
        StoredFile image = adminReceiptService.getImage(recptSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }
}
