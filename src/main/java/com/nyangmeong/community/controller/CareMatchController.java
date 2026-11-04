package com.nyangmeong.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.CareCompletionRequestCreateRequest;
import com.nyangmeong.community.dto.CareCompletionRequestResponse;
import com.nyangmeong.community.dto.CareMatchCancelRequest;
import com.nyangmeong.community.dto.CareMatchCreateRequest;
import com.nyangmeong.community.dto.CareMatchReceiptConfirmRequest;
import com.nyangmeong.community.dto.CareMatchResponse;
import com.nyangmeong.community.service.CareMatchService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 매칭·거래 컨트롤러]
 * - SCR-CMU-005 매칭 요청 -> 수락 -> 돌봄 완료 요청 -> 완료 확인 -> 수령 확인
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/care-matches")
public class CareMatchController {

    private final CareMatchService careMatchService;

    @Operation(summary = "매칭 조건·완료 요청 회차·수령 상태 조회(참여자만)")
    @GetMapping("/{petPrtcMtchSn}")
    public ResponseEntity<ApiResponse<CareMatchResponse>> getMatch(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcMtchSn) {
        return ResponseEntity.ok(ApiResponse.success(careMatchService.getMatch(me.id(), petPrtcMtchSn)));
    }

    @Operation(summary = "매칭 요청(보호자)")
    @PostMapping
    public ResponseEntity<ApiResponse<CareMatchResponse>> request(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody CareMatchCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(careMatchService.request(me.id(), req)));
    }

    @Operation(summary = "수락 전 매칭 요청 취소(보호자)")
    @PostMapping("/{petPrtcMtchSn}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcMtchSn, @Valid @RequestBody CareMatchCancelRequest req) {
        careMatchService.cancel(me.id(), petPrtcMtchSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "매칭 수락(제공자)")
    @PostMapping("/{petPrtcMtchSn}/accept")
    public ResponseEntity<ApiResponse<Void>> accept(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcMtchSn) {
        careMatchService.accept(me.id(), petPrtcMtchSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "돌봄 완료 요청(제공자)")
    @PostMapping("/{petPrtcMtchSn}/completion-requests")
    public ResponseEntity<ApiResponse<CareCompletionRequestResponse>> requestCompletion(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long petPrtcMtchSn,
            @Valid @RequestBody CareCompletionRequestCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(careMatchService.requestCompletion(me.id(), petPrtcMtchSn, req)));
    }

    @Operation(summary = "돌봄 완료 확인(보호자)")
    @PostMapping("/{petPrtcMtchSn}/confirm-completion")
    public ResponseEntity<ApiResponse<Void>> confirmCompletion(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcMtchSn) {
        careMatchService.confirmCompletion(me.id(), petPrtcMtchSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "비용 수령 확인(제공자, 받았으면 거래 완료)")
    @PostMapping("/{petPrtcMtchSn}/confirm-receipt")
    public ResponseEntity<ApiResponse<Void>> confirmReceipt(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petPrtcMtchSn, @Valid @RequestBody CareMatchReceiptConfirmRequest req) {
        careMatchService.confirmReceipt(me.id(), petPrtcMtchSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
