package com.nyangmeong.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.AdminPostDetailResponse;
import com.nyangmeong.community.dto.AdminPostSearchCond;
import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.dto.AdminPostSummary;
import com.nyangmeong.community.service.AdminPostService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 게시글 컨트롤러]
 * - SCR-ADM-020 (FN-ADM-020-01~05)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @Operation(summary = "게시글 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPostSummary>>> search(AdminPostSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.search(cond, pq)));
    }

    @Operation(summary = "게시글 상세")
    @GetMapping("/{pstSn}")
    public ResponseEntity<ApiResponse<AdminPostDetailResponse>> get(@PathVariable Long pstSn) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.get(pstSn)));
    }

    @Operation(summary = "게시글 블라인드")
    @PostMapping("/{pstSn}/blind")
    public ResponseEntity<ApiResponse<Void>> blind(@AuthenticationPrincipal LoginMember me, @PathVariable Long pstSn,
            @Valid @RequestBody AdminPostStatusChangeRequest req) {
        adminPostService.blind(me.id(), pstSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "게시글 블라인드 복구")
    @PostMapping("/{pstSn}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn, @Valid @RequestBody AdminPostStatusChangeRequest req) {
        adminPostService.restore(me.id(), pstSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
