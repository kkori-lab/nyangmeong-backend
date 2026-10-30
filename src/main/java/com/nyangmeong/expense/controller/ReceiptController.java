package com.nyangmeong.expense.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ReceiptResultResponse;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [영수증 컨트롤러]
 * - 분석 결과 다시 열기·원본 이미지 (SCR-AIM-008·013, 확인 화면은 SCR-HLT-002)
 * - 업로드·분석은 Python AI 서버 (내부 API ReceiptInternalController)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptAnalyzer receiptAnalyzer;
    private final ReceiptService receiptService;

    @Operation(summary = "영수증 분석 결과 다시 열기")
    @GetMapping("/{recptSn}")
    public ResponseEntity<ApiResponse<ReceiptResultResponse>> get(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long recptSn, @RequestParam Long petSn) {
        return ResponseEntity.ok(ApiResponse.success(receiptAnalyzer.getResult(me.id(), recptSn, petSn)));
    }

    @Operation(summary = "영수증 원본 이미지")
    @GetMapping("/{recptSn}/image")
    public ResponseEntity<byte[]> getImage(@AuthenticationPrincipal LoginMember me, @PathVariable Long recptSn) {
        StoredFile image = receiptService.getImage(me.id(), recptSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }
}
