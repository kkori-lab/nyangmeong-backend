package com.nyangmeong.global.external;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 외부 API 사용량 컨트롤러]
 * - SCR-ADM-112 (FN-ADM-112-01·02·03)
 * - 조회만 제공 : 로그는 영구 보관 (FN-ADM-112-04)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-api-usage")
public class AdminExternalApiUsageController {

    private final ExternalApiUsageService externalApiUsageService;

    @Operation(summary = "외부 API 종류별 사용량")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExternalApiUsageSummary>>> search(ExternalApiUsageSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(externalApiUsageService.search(cond)));
    }

    @Operation(summary = "외부 API 일별 사용량")
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<ExternalApiDailyUsage>>> searchDaily(ExternalApiUsageSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(externalApiUsageService.searchDaily(cond)));
    }

    @Operation(summary = "AI 일별 토큰 사용량")
    @GetMapping("/tokens")
    public ResponseEntity<ApiResponse<List<ExternalApiDailyToken>>> searchTokens(ExternalApiUsageSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(externalApiUsageService.searchTokens(cond)));
    }

    @Operation(summary = "외부 API 실패 목록")
    @GetMapping("/failures")
    public ResponseEntity<ApiResponse<PageResponse<ExternalApiFailureSummary>>> searchFailures(
            ExternalApiUsageSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(externalApiUsageService.searchFailures(cond, pq)));
    }
}
