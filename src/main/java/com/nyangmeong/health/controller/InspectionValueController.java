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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.InspectionValueCreateRequest;
import com.nyangmeong.health.dto.InspectionValueResponse;
import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.dto.InspectionValueUpdateRequest;
import com.nyangmeong.health.service.InspectionValueService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [검사 수치 컨트롤러]
 * - SCR-HLT-005 혈액검사 결과 목록·선택 삭제, SCR-HLT-007 검사 수치 저장·수정 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/inspection-values")
public class InspectionValueController {

    private final InspectionValueService inspectionValueService;

    @Operation(summary = "검사 수치 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionValueSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute InspectionValueSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(inspectionValueService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "검사 수치 선택 삭제")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @RequestParam("inspNvlSn") List<Long> inspNvlSns) {
        inspectionValueService.delete(me.id(), petSn, inspNvlSns);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "검사 수치 저장 (이미 등록된 항목 제외, 삭제된 같은 항목 복구)")
    @PostMapping
    public ResponseEntity<ApiResponse<List<InspectionValueResponse>>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody InspectionValueCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(inspectionValueService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "검사 수치 수정")
    @PatchMapping("/{inspNvlSn}")
    public ResponseEntity<ApiResponse<InspectionValueResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long inspNvlSn,
            @Valid @RequestBody InspectionValueUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(inspectionValueService.update(me.id(), petSn, inspNvlSn, req)));
    }
}
