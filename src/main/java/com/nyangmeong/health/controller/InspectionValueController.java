package com.nyangmeong.health.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.service.InspectionValueService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [검사 수치 컨트롤러]
 * - SCR-HLT-005 혈액검사 결과 목록·선택 삭제 (docs/api/health.md)
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
}
