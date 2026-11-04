package com.nyangmeong.member.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.member.dto.AdminTermsCreateRequest;
import com.nyangmeong.member.dto.AdminTermsDetail;
import com.nyangmeong.member.dto.AdminTermsSummary;
import com.nyangmeong.member.service.AdminTermsService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 약관 컨트롤러]
 * - SCR-ADM-051 (FN-ADM-051-01·02·03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/terms")
public class AdminTermsController {

    private final AdminTermsService adminTermsService;

    @Operation(summary = "관리자 약관 목록 (종류별 사용 중 버전)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminTermsSummary>>> search() {
        return ResponseEntity.ok(ApiResponse.success(adminTermsService.search()));
    }

    @Operation(summary = "관리자 약관 버전 이력")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AdminTermsSummary>>> searchHistory(
            @RequestParam String trmsFixIdntfKey) {
        return ResponseEntity.ok(ApiResponse.success(adminTermsService.searchHistory(trmsFixIdntfKey)));
    }

    @Operation(summary = "관리자 약관 상세")
    @GetMapping("/{trmsSn}")
    public ResponseEntity<ApiResponse<AdminTermsDetail>> get(@PathVariable Long trmsSn) {
        return ResponseEntity.ok(ApiResponse.success(adminTermsService.get(trmsSn)));
    }

    @Operation(summary = "약관 새 버전 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminTermsDetail>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminTermsCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminTermsService.create(me.id(), req)));
    }
}
