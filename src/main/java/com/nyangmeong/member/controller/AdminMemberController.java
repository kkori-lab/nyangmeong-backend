package com.nyangmeong.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.member.dto.AdminMemberReleaseRequest;
import com.nyangmeong.member.dto.AdminMemberSearchCond;
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.dto.AdminSuspensionSearchCond;
import com.nyangmeong.member.dto.AdminSuspensionSummary;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSuspensionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 회원 컨트롤러]
 * - SCR-ADM-010 (FN-ADM-010-01~03), SCR-ADM-012 (FN-ADM-012-01·03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;
    private final MemberSuspensionService memberSuspensionService;

    @Operation(summary = "회원 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberSummary>>> searchForAdmin(AdminMemberSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberService.searchForAdmin(cond, pq)));
    }

    @Operation(summary = "정지 회원 목록")
    @GetMapping("/suspensions")
    public ResponseEntity<ApiResponse<PageResponse<AdminSuspensionSummary>>> searchSuspensions(
            AdminSuspensionSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberSuspensionService.searchSuspensions(cond, pq)));
    }

    @Operation(summary = "회원 정지")
    @PostMapping("/{mbrSn}/suspension")
    public ResponseEntity<ApiResponse<Void>> suspend(@AuthenticationPrincipal LoginMember me, @PathVariable Long mbrSn,
            @Valid @RequestBody AdminMemberSuspendRequest req) {
        memberSuspensionService.suspend(me.id(), mbrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원 정지 해제")
    @DeleteMapping("/{mbrSn}/suspension")
    public ResponseEntity<ApiResponse<Void>> release(@AuthenticationPrincipal LoginMember me, @PathVariable Long mbrSn,
            @Valid @RequestBody AdminMemberReleaseRequest req) {
        memberSuspensionService.release(me.id(), mbrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
