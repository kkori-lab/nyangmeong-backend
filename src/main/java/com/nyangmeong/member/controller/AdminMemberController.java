package com.nyangmeong.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.member.dto.AdminMemberSearchCond;
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [관리자 회원 컨트롤러]
 * - SCR-ADM-010 (FN-ADM-010-01~03)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberSummary>>> searchForAdmin(AdminMemberSearchCond cond,
            PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(memberService.searchForAdmin(cond, pq)));
    }
}
