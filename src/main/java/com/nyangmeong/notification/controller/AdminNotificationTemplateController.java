package com.nyangmeong.notification.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.notification.dto.AdminNotificationTemplateCreateRequest;
import com.nyangmeong.notification.dto.AdminNotificationTemplateCreateResponse;
import com.nyangmeong.notification.dto.AdminNotificationTemplateSummary;
import com.nyangmeong.notification.dto.AdminNotificationTemplateUpdateRequest;
import com.nyangmeong.notification.service.AdminNotificationTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 알림 템플릿 컨트롤러]
 * - SCR-ADM-090 (FN-ADM-090-01~02)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notification-templates")
public class AdminNotificationTemplateController {

    private final AdminNotificationTemplateService adminNotificationTemplateService;

    @Operation(summary = "관리자 알림 템플릿 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminNotificationTemplateSummary>>> search(
            @RequestParam(required = false) String notiChnlCd) {
        return ResponseEntity.ok(ApiResponse.success(adminNotificationTemplateService.search(notiChnlCd)));
    }

    @Operation(summary = "알림 템플릿 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminNotificationTemplateCreateResponse>> create(
            @AuthenticationPrincipal LoginMember me, @Valid @RequestBody AdminNotificationTemplateCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminNotificationTemplateService.create(me.id(), req)));
    }

    @Operation(summary = "알림 템플릿 수정")
    @PatchMapping("/{notiTmpltSn}")
    public ResponseEntity<ApiResponse<Void>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long notiTmpltSn, @Valid @RequestBody AdminNotificationTemplateUpdateRequest req) {
        adminNotificationTemplateService.update(me.id(), notiTmpltSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
