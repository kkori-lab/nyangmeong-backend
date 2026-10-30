package com.nyangmeong.global.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 운영 설정 컨트롤러]
 * - SCR-ADM-110 (FN-ADM-110-01~04)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
public class AdminSystemSettingController {

    private final AdminSystemSettingService adminSystemSettingService;

    @Operation(summary = "운영 설정 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminSettingResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(AdminSettingResponse.from(adminSystemSettingService.get())));
    }

    @Operation(summary = "운영 설정 수정")
    @PatchMapping
    public ResponseEntity<ApiResponse<AdminSettingResponse>> update(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminSettingUpdateRequest req) {
        return ResponseEntity
                .ok(ApiResponse.success(AdminSettingResponse.from(adminSystemSettingService.update(me, req))));
    }
}
