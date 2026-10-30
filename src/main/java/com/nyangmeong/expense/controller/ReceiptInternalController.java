package com.nyangmeong.expense.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ReceiptOriginalRequest;
import com.nyangmeong.expense.dto.ReceiptOriginalResponse;
import com.nyangmeong.expense.dto.ReceiptReportRequest;
import com.nyangmeong.expense.dto.ReceiptReportResponse;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [영수증 내부 API 컨트롤러]
 * - /internal/ai/receipts : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출 (docs/api/receipt.md)
 * - 분석은 Python, Java 는 원본 저장과 결과·상태 반영만 (FN-AIM-008-09·010-01·013-04·08)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ai/receipts")
public class ReceiptInternalController {

    private final ReceiptService receiptService;
    private final ReceiptAnalyzer receiptAnalyzer;

    @Operation(summary = "영수증 원본 저장(분석 전)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReceiptOriginalResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute ReceiptOriginalRequest req) {
        Long recptSn = receiptService.create(me.id(), req.petSn(), req.file(), req.ocrDmndSrcCd());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(new ReceiptOriginalResponse(recptSn)));
    }

    @Operation(summary = "영수증 분석 결과·상태 보고")
    @PostMapping("/{recptSn}/result")
    public ResponseEntity<ApiResponse<ReceiptReportResponse>> report(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long recptSn, @Valid @RequestBody ReceiptReportRequest req) {
        return ResponseEntity.ok(ApiResponse.success(receiptAnalyzer.report(me.id(), recptSn, req.ocrRslt(),
                req.ocr())));
    }
}
