package com.nyangmeong.health.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.RepeatPreventionCreateRequest;
import com.nyangmeong.health.dto.RepeatPreventionResponse;
import com.nyangmeong.health.dto.RepeatPreventionSearchCond;
import com.nyangmeong.health.dto.RepeatPreventionSummary;
import com.nyangmeong.health.service.RepeatPreventionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [반복 예방 관리 컨트롤러]
 * - SCR-HLT-005 반복 예방 목록·등록·해지 (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/repeat-preventions")
public class RepeatPreventionController {

    private final RepeatPreventionService repeatPreventionService;

    @Operation(summary = "반복 예방 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepeatPreventionSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute RepeatPreventionSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(repeatPreventionService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "반복 예방 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<RepeatPreventionResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody RepeatPreventionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(repeatPreventionService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "반복 예방 해지")
    @DeleteMapping("/{reptPrvntMngSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long reptPrvntMngSn) {
        repeatPreventionService.delete(me.id(), petSn, reptPrvntMngSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
