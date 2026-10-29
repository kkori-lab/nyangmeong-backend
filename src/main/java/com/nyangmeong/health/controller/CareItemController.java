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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.CareCompletionCreateRequest;
import com.nyangmeong.health.dto.CareCompletionResponse;
import com.nyangmeong.health.dto.CareCompletionSearchCond;
import com.nyangmeong.health.dto.CareCompletionSummary;
import com.nyangmeong.health.dto.CareItemCreateRequest;
import com.nyangmeong.health.dto.CareItemExposureSaveRequest;
import com.nyangmeong.health.dto.CareItemResponse;
import com.nyangmeong.health.dto.CareItemSearchCond;
import com.nyangmeong.health.dto.CareItemSummary;
import com.nyangmeong.health.dto.CareItemUpdateRequest;
import com.nyangmeong.health.service.CareItemService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [토탈 케어리스트 컨트롤러]
 * - SCR-HLT-003 케어 항목·완료 기록·메인 표시, 메인페이지 MAN-009도 메인 표시 저장 API 사용 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}")
public class CareItemController {

    private final CareItemService careItemService;

    @Operation(summary = "케어 항목 목록(토탈 케어리스트)")
    @GetMapping("/care-items")
    public ResponseEntity<ApiResponse<List<CareItemSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @ModelAttribute CareItemSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(careItemService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "케어 항목 추가")
    @PostMapping("/care-items")
    public ResponseEntity<ApiResponse<CareItemResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody CareItemCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(careItemService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "케어 항목 수정")
    @PatchMapping("/care-items/{careArtclSn}")
    public ResponseEntity<ApiResponse<CareItemResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long careArtclSn, @Valid @RequestBody CareItemUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(careItemService.update(me.id(), petSn, careArtclSn, req)));
    }

    @Operation(summary = "케어 항목 삭제")
    @DeleteMapping("/care-items/{careArtclSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long careArtclSn) {
        careItemService.delete(me.id(), petSn, careArtclSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "메인 표시 항목 전체 저장")
    @PutMapping("/care-item-exposures")
    public ResponseEntity<ApiResponse<List<CareItemSummary>>> save(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody CareItemExposureSaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(careItemService.save(me.id(), petSn, req)));
    }

    @Operation(summary = "케어 완료 기록 목록")
    @GetMapping("/care-completions")
    public ResponseEntity<ApiResponse<List<CareCompletionSummary>>> searchCompletions(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petSn,
            @ModelAttribute CareCompletionSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(careItemService.searchCompletions(me.id(), petSn, cond)));
    }

    @Operation(summary = "케어 완료 체크")
    @PostMapping("/care-completions")
    public ResponseEntity<ApiResponse<CareCompletionResponse>> complete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody CareCompletionCreateRequest req) {
        CareItemService.Completed completed = careItemService.complete(me.id(), petSn, req);
        if (completed.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(completed.response()));
        }
        return ResponseEntity.ok(ApiResponse.success(completed.response()));
    }

    @Operation(summary = "케어 완료 취소")
    @DeleteMapping("/care-completions/{careCmptnHstrySn}")
    public ResponseEntity<ApiResponse<Void>> cancel(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long careCmptnHstrySn) {
        careItemService.cancel(me.id(), petSn, careCmptnHstrySn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
