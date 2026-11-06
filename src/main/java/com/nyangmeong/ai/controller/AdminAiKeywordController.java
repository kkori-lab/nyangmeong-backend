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

import com.nyangmeong.ai.dto.AdminAiKeywordCreateResponse;
import com.nyangmeong.ai.dto.AdminAiKeywordSaveRequest;
import com.nyangmeong.ai.dto.AdminAiKeywordSummary;
import com.nyangmeong.ai.service.AdminAiKeywordService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 키워드 사전 컨트롤러]
 * - SCR-ADM-081 (FN-ADM-081-01·02)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai/keywords")
public class AdminAiKeywordController {

    private final AdminAiKeywordService adminAiKeywordService;

    @Operation(summary = "키워드 사전 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminAiKeywordSummary>>> search(@RequestParam String dictUsgCd,
            @RequestParam(required = false) String kwrdClsfNm) {
        return ResponseEntity.ok(ApiResponse.success(adminAiKeywordService.search(dictUsgCd, kwrdClsfNm)));
    }

    @Operation(summary = "키워드 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminAiKeywordCreateResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody AdminAiKeywordSaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminAiKeywordService.create(me.id(), req)));
    }

    @Operation(summary = "키워드 수정")
    @PatchMapping("/{kwrdDictSn}")
    public ResponseEntity<ApiResponse<AdminAiKeywordSummary>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long kwrdDictSn, @Valid @RequestBody AdminAiKeywordSaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminAiKeywordService.update(me.id(), kwrdDictSn, req)));
    }

    @Operation(summary = "키워드 삭제")
    @DeleteMapping("/{kwrdDictSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long kwrdDictSn) {
        adminAiKeywordService.delete(me.id(), kwrdDictSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
