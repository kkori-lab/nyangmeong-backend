package com.nyangmeong.global.batch;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 배치 현황 컨트롤러]
 * - SCR-ADM-111 (FN-ADM-111-01·02·03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/batches")
public class AdminBatchController {

    private final AdminBatchService adminBatchService;

    @Operation(summary = "배치 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchSummary>>> search() {
        return ResponseEntity.ok(ApiResponse.success(adminBatchService.search()));
    }

    @Operation(summary = "배치 실행 이력")
    @GetMapping("/{btchSeCd}/executions")
    public ResponseEntity<ApiResponse<PageResponse<BatchExecutionSummary>>> searchExecutions(
            @PathVariable String btchSeCd, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminBatchService.searchExecutions(btchSeCd, pq)));
    }

    @Operation(summary = "배치 수동 실행")
    @PostMapping("/{btchSeCd}/run")
    public ResponseEntity<ApiResponse<BatchRunResponse>> run(@AuthenticationPrincipal LoginMember me,
            @PathVariable String btchSeCd) {
        return ResponseEntity.ok(ApiResponse.success(adminBatchService.run(me.id(), btchSeCd)));
    }
}
