package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.AdminCareMissionSummary;
import com.nyangmeong.pet.dto.AdminCareMissionUpdateRequest;
import com.nyangmeong.pet.service.AdminCareMissionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 케어미션 컨트롤러]
 * - SCR-ADM-071 (FN-ADM-071-01·02)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/care-missions")
public class AdminCareMissionController {

    private final AdminCareMissionService adminCareMissionService;

    @Operation(summary = "관리자 케어미션 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCareMissionSummary>>> search(
            @RequestParam(required = false) String msnPoolCd) {
        return ResponseEntity.ok(ApiResponse.success(adminCareMissionService.search(msnPoolCd)));
    }

    @Operation(summary = "케어미션 수정")
    @PatchMapping("/{careMsnSn}")
    public ResponseEntity<ApiResponse<AdminCareMissionSummary>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long careMsnSn, @Valid @RequestBody AdminCareMissionUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminCareMissionService.update(me.id(), careMsnSn, req)));
    }
}
