package com.nyangmeong.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.notification.dto.AdminNotificationLogSearchCond;
import com.nyangmeong.notification.dto.AdminNotificationLogSummary;
import com.nyangmeong.notification.service.AdminNotificationLogService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 알림 발송 이력 컨트롤러]
 * - SCR-ADM-091 (FN-ADM-091-01) 조회만 : 재발송 API는 없다 (FN-ADM-091-02)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notification-logs")
public class AdminNotificationLogController {

    private final AdminNotificationLogService adminNotificationLogService;

    @Operation(summary = "관리자 알림 발송 이력 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminNotificationLogSummary>>> search(
            AdminNotificationLogSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminNotificationLogService.search(cond, pq)));
    }
}
