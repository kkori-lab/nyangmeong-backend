package com.nyangmeong.expense.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ExpenseCreateRequest;
import com.nyangmeong.expense.dto.ExpenseResponse;
import com.nyangmeong.expense.dto.ExpenseSummary;
import com.nyangmeong.expense.service.ExpenseService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [가계부 지출 컨트롤러]
 * - 지출 등록·저장 전 중복 확인 (SCR-MYP-031)
 * - 같은 멱등 요청 ID 의 재전송은 201 이 아니라 200 으로 같은 결과
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "가계부 지출 등록(반려동물별 줄, 영수증 연결)")
    @PostMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ExpenseCreateRequest req) {
        ExpenseService.Created created = expenseService.create(me.id(), req);
        if (created.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created.responses()));
        }
        return ResponseEntity.ok(ApiResponse.success(created.responses()));
    }

    @Operation(summary = "가계부 지출 저장 전 중복 확인")
    @PostMapping("/duplicate-check")
    public ResponseEntity<ApiResponse<List<ExpenseSummary>>> searchDuplicates(
            @AuthenticationPrincipal LoginMember me, @Valid @RequestBody ExpenseCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.searchDuplicates(me.id(), req)));
    }
}
