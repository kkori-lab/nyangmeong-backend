package com.nyangmeong.health.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.CheckupCreateRequest;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSearchCond;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.CheckupUpdateRequest;
import com.nyangmeong.health.service.CheckupService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [검진기록 컨트롤러]
 * - SCR-HLT-002 검진기록 목록·직접 등록·수정·삭제, SCR-HLT-005 기본정보 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/checkups")
public class CheckupController {

    private final CheckupService checkupService;

    @Operation(summary = "검진기록 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CheckupSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute CheckupSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(checkupService.search(me.id(), petSn, cond, pq)));
    }

    @Operation(summary = "검진상세 기본정보")
    @GetMapping("/{ckupRcdSn}")
    public ResponseEntity<ApiResponse<CheckupResponse>> getCheckup(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long ckupRcdSn) {
        return ResponseEntity.ok(ApiResponse.success(checkupService.getCheckup(me.id(), petSn, ckupRcdSn)));
    }

    @Operation(summary = "검진기록 직접 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<CheckupResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody CheckupCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(checkupService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "검진기록 수정")
    @PatchMapping("/{ckupRcdSn}")
    public ResponseEntity<ApiResponse<CheckupResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long ckupRcdSn, @Valid @RequestBody CheckupUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(checkupService.update(me.id(), petSn, ckupRcdSn, req)));
    }

    @Operation(summary = "검진기록 삭제")
    @DeleteMapping("/{ckupRcdSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long ckupRcdSn) {
        checkupService.delete(me.id(), petSn, ckupRcdSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
