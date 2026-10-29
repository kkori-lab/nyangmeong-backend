package com.nyangmeong.expense.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ExpenseLimitResponse;
import com.nyangmeong.expense.dto.ExpenseLimitSaveRequest;
import com.nyangmeong.expense.service.ExpenseLimitService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [월 지출 한도 컨트롤러]
 * - 회원당 하나라 /members/me 아래 (FN-MYP-030-13)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/expense-limit")
public class ExpenseLimitController {

    private final ExpenseLimitService expenseLimitService;

    @Operation(summary = "월 지출 한도 저장(처음 저장하면 한도 알림 수신을 켬)")
    @PutMapping
    public ResponseEntity<ApiResponse<ExpenseLimitResponse>> save(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ExpenseLimitSaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(expenseLimitService.save(me.id(), req)));
    }
}
