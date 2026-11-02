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
import com.nyangmeong.member.dto.AdminAccountSummary;
import com.nyangmeong.member.dto.AdminMemberReleaseRequest;
import com.nyangmeong.member.dto.AdminMemberSearchCond;
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.dto.AdminSuspensionSearchCond;
import com.nyangmeong.member.dto.AdminSuspensionSummary;
import com.nyangmeong.member.dto.AdminWithdrawnMemberSummary;
import com.nyangmeong.member.service.MemberAuthorityService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSuspensionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 회원 컨트롤러]
 * - SCR-ADM-010 (FN-ADM-010-01~03), SCR-ADM-012 (FN-ADM-012-01·03), SCR-ADM-013 (FN-ADM-013-01)
 * - SCR-ADM-113 (FN-ADM-113-01~03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminMemberController {

    private final MemberService memberService;
    private final MemberSuspensionService memberSuspensionService;
    private final MemberAuthorityService memberAuthorityService;

    @Operation(summary = "회원 목록")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberSummary>>> searchForAdmin(AdminMemberSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberService.searchForAdmin(cond, pq)));
    }

    @Operation(summary = "정지 회원 목록")
    @GetMapping("/members/suspensions")
    public ResponseEntity<ApiResponse<PageResponse<AdminSuspensionSummary>>> searchSuspensions(
            AdminSuspensionSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberSuspensionService.searchSuspensions(cond, pq)));
    }

    @Operation(summary = "탈퇴 회원 목록")
    @GetMapping("/members/withdrawn")
    public ResponseEntity<ApiResponse<PageResponse<AdminWithdrawnMemberSummary>>> searchWithdrawn(PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberService.searchWithdrawnForAdmin(pq)));
    }

    @Operation(summary = "회원 정지")
    @PostMapping("/members/{mbrSn}/suspension")
    public ResponseEntity<ApiResponse<Void>> suspend(@AuthenticationPrincipal LoginMember me, @PathVariable Long mbrSn,
            @Valid @RequestBody AdminMemberSuspendRequest req) {
        memberSuspensionService.suspend(me.id(), mbrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원 정지 해제")
    @DeleteMapping("/members/{mbrSn}/suspension")
    public ResponseEntity<ApiResponse<Void>> release(@AuthenticationPrincipal LoginMember me, @PathVariable Long mbrSn,
            @Valid @RequestBody AdminMemberReleaseRequest req) {
        memberSuspensionService.release(me.id(), mbrSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "관리자 목록")
    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<PageResponse<AdminAccountSummary>>> searchAdmins(PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberAuthorityService.searchAdmins(pq)));
    }

    @Operation(summary = "관리자 지정")
    @PostMapping("/admins/{mbrSn}")
    public ResponseEntity<ApiResponse<Void>> appointAdmin(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long mbrSn) {
        memberAuthorityService.appointAdmin(me.id(), mbrSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "관리자 해제")
    @DeleteMapping("/admins/{mbrSn}")
    public ResponseEntity<ApiResponse<Void>> releaseAdmin(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long mbrSn) {
        memberAuthorityService.releaseAdmin(me.id(), mbrSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
