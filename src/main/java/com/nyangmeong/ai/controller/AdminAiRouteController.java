package com.nyangmeong.ai.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.AdminAiRouteCreateResponse;
import com.nyangmeong.ai.dto.AdminAiRouteSaveRequest;
import com.nyangmeong.ai.dto.AdminAiRouteSummary;
import com.nyangmeong.ai.service.AdminAiRouteService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 라우팅 매핑표 컨트롤러]
 * - SCR-ADM-080 (FN-ADM-080-01·02·03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai/routes")
public class AdminAiRouteController {

    private final AdminAiRouteService adminAiRouteService;

    @Operation(summary = "라우팅 매핑표 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminAiRouteSummary>>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(adminAiRouteService.search(q)));
    }

    @Operation(summary = "라우팅 매핑 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminAiRouteCreateResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminAiRouteSaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminAiRouteService.create(me.id(), req)));
    }

    @Operation(summary = "라우팅 매핑 수정")
    @PatchMapping("/{aiRoutMpngSn}")
    public ResponseEntity<ApiResponse<AdminAiRouteSummary>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long aiRoutMpngSn, @Valid @RequestBody AdminAiRouteSaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminAiRouteService.update(me.id(), aiRoutMpngSn, req)));
    }

    @Operation(summary = "라우팅 매핑 삭제")
    @DeleteMapping("/{aiRoutMpngSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long aiRoutMpngSn) {
        adminAiRouteService.delete(me.id(), aiRoutMpngSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
