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
import com.nyangmeong.health.dto.PrescriptionCreateRequest;
import com.nyangmeong.health.dto.PrescriptionResponse;
import com.nyangmeong.health.dto.PrescriptionSearchCond;
import com.nyangmeong.health.dto.PrescriptionSummary;
import com.nyangmeong.health.dto.PrescriptionUpdateRequest;
import com.nyangmeong.health.service.PrescriptionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [처방약 컨트롤러]
 * - SCR-HLT-005 처방약 목록·추가·수정·삭제 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "처방약 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PrescriptionSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute PrescriptionSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "처방약 추가")
    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody PrescriptionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(prescriptionService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "처방약 수정")
    @PatchMapping("/{prscSn}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long prscSn, @Valid @RequestBody PrescriptionUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.update(me.id(), petSn, prscSn, req)));
    }

    @Operation(summary = "처방약 삭제")
    @DeleteMapping("/{prscSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long prscSn) {
        prescriptionService.delete(me.id(), petSn, prscSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
