package com.nyangmeong.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.AdminCommentDeleteRequest;
import com.nyangmeong.community.dto.AdminCommentSearchCond;
import com.nyangmeong.community.dto.AdminCommentSummary;
import com.nyangmeong.community.service.AdminCommentService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 댓글 컨트롤러]
 * - SCR-ADM-020 (FN-ADM-020-06)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @Operation(summary = "댓글 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCommentSummary>>> search(AdminCommentSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(adminCommentService.search(cond, pq)));
    }

    @Operation(summary = "댓글 삭제")
    @PostMapping("/{cmntSn}/deletion")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long cmntSn, @Valid @RequestBody AdminCommentDeleteRequest req) {
        adminCommentService.delete(me.id(), cmntSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
