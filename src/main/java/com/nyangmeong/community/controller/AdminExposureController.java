package com.nyangmeong.community.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.AdminCommentarySummary;
import com.nyangmeong.community.dto.AdminCommentaryUseRequest;
import com.nyangmeong.community.dto.AdminHealthTipAssignRequest;
import com.nyangmeong.community.dto.AdminHealthTipResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseCreateRequest;
import com.nyangmeong.community.dto.AdminTodayPhraseCreateResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseListResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseUpdateRequest;
import com.nyangmeong.community.service.AdminExposureService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 노출 콘텐츠 컨트롤러]
 * - SCR-ADM-023 (FN-ADM-023-01~06), 오늘의 한마디 선택(05)은 배치 SYSC0030
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/community/exposure")
public class AdminExposureController {

    private final AdminExposureService adminExposureService;

    @Operation(summary = "오늘의 건강팁 현황")
    @GetMapping("/health-tips")
    public ResponseEntity<ApiResponse<List<AdminHealthTipResponse>>> getHealthTips() {
        return ResponseEntity.ok(ApiResponse.success(adminExposureService.getHealthTips()));
    }

    @Operation(summary = "건강팁 지정")
    @PutMapping("/health-tips/{pstSn}")
    public ResponseEntity<ApiResponse<Void>> assignHealthTip(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn, @Valid @RequestBody AdminHealthTipAssignRequest req) {
        adminExposureService.assignHealthTip(me.id(), pstSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "건강팁 지정 해제")
    @DeleteMapping("/health-tips/{pstSn}")
    public ResponseEntity<ApiResponse<Void>> releaseHealthTip(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn) {
        adminExposureService.releaseHealthTip(me.id(), pstSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "오늘의 한마디 후보 문구 목록")
    @GetMapping("/phrases")
    public ResponseEntity<ApiResponse<AdminTodayPhraseListResponse>> getPhrases() {
        return ResponseEntity.ok(ApiResponse.success(adminExposureService.getPhrases()));
    }

    @Operation(summary = "오늘의 한마디 후보 문구 등록")
    @PostMapping("/phrases")
    public ResponseEntity<ApiResponse<AdminTodayPhraseCreateResponse>> createPhrase(
            @AuthenticationPrincipal LoginMember me, @Valid @RequestBody AdminTodayPhraseCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminExposureService.createPhrase(me.id(), req)));
    }

    @Operation(summary = "오늘의 한마디 후보 문구 수정")
    @PatchMapping("/phrases/{todayPhrsSn}")
    public ResponseEntity<ApiResponse<Void>> updatePhrase(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long todayPhrsSn, @Valid @RequestBody AdminTodayPhraseUpdateRequest req) {
        adminExposureService.updatePhrase(me.id(), todayPhrsSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "AI 코멘트 목록")
    @GetMapping("/commentaries")
    public ResponseEntity<ApiResponse<PageResponse<AdminCommentarySummary>>> searchCommentaries(PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminExposureService.searchCommentaries(pq)));
    }

    @Operation(summary = "AI 코멘트 사용 여부 변경")
    @PatchMapping("/commentaries/{cmntyAiCmtrySn}")
    public ResponseEntity<ApiResponse<Void>> changeCommentaryUse(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long cmntyAiCmtrySn, @Valid @RequestBody AdminCommentaryUseRequest req) {
        adminExposureService.changeCommentaryUse(me.id(), cmntyAiCmtrySn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
