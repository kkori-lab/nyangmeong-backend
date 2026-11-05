package com.nyangmeong.global.external;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 검사결과지 분석 현황 컨트롤러]
 * - SCR-ADM-082 (FN-ADM-082-03)
 * - Python 이 통지한 PDF 분석 호출(SYSC0025) 기준 건수와 실패 목록
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/processing/pdf-analyses")
public class AdminPdfAnalysisController {

    private final ExternalApiUsageService externalApiUsageService;

    @Operation(summary = "검사결과지 분석 현황")
    @GetMapping
    public ResponseEntity<ApiResponse<PdfAnalysisListResponse>> search(PdfAnalysisSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(externalApiUsageService.searchPdfAnalyses(cond, pq)));
    }
}
