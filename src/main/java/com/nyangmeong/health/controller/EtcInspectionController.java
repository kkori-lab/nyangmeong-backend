package com.nyangmeong.health.controller;

import java.util.List;

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
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.EtcInspectionCreateRequest;
import com.nyangmeong.health.dto.EtcInspectionResponse;
import com.nyangmeong.health.dto.EtcInspectionSearchCond;
import com.nyangmeong.health.dto.EtcInspectionSummary;
import com.nyangmeong.health.dto.EtcInspectionUpdateRequest;
import com.nyangmeong.health.service.EtcInspectionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [기타검사 컨트롤러]
 * - SCR-HLT-005 기타검사 목록·입력·수정·삭제 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/etc-inspections")
public class EtcInspectionController {

    private final EtcInspectionService etcInspectionService;

    @Operation(summary = "기타검사 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EtcInspectionSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute EtcInspectionSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(etcInspectionService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "기타검사 입력")
    @PostMapping
    public ResponseEntity<ApiResponse<EtcInspectionResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody EtcInspectionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(etcInspectionService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "기타검사 수정")
    @PatchMapping("/{etcInspSn}")
    public ResponseEntity<ApiResponse<EtcInspectionResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long etcInspSn,
            @Valid @RequestBody EtcInspectionUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(etcInspectionService.update(me.id(), petSn, etcInspSn, req)));
    }

    @Operation(summary = "기타검사 삭제")
    @DeleteMapping("/{etcInspSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long etcInspSn) {
        etcInspectionService.delete(me.id(), petSn, etcInspSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
