package com.nyangmeong.global.log;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 감사 로그 컨트롤러]
 * - SCR-ADM-114 (FN-ADM-114-01·02·04)
 * - 조회만 제공 : 수정·삭제 API 없음
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AdminTaskLogController {

    private final AdminTaskLogService adminTaskLogService;

    @Operation(summary = "감사 로그 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminTaskLogSummary>>> search(AdminTaskLogSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminTaskLogService.search(cond, pq)));
    }

    @Operation(summary = "감사 로그 관리자 목록")
    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<List<AdminTaskLogAdminSummary>>> searchAdmins() {
        return ResponseEntity.ok(ApiResponse.success(adminTaskLogService.searchAdmins()));
    }

    @Operation(summary = "감사 로그 상세")
    @GetMapping("/{mngrTaskLogSn}")
    public ResponseEntity<ApiResponse<AdminTaskLogDetailResponse>> getDetail(@PathVariable Long mngrTaskLogSn) {
        return ResponseEntity.ok(ApiResponse.success(adminTaskLogService.getDetail(mngrTaskLogSn)));
    }
}
