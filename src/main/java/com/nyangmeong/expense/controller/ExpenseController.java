package com.nyangmeong.expense.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ExpenseBulkDeleteRequest;
import com.nyangmeong.expense.dto.ExpenseCreateRequest;
import com.nyangmeong.expense.dto.ExpenseMonthSummary;
import com.nyangmeong.expense.dto.ExpensePetOption;
import com.nyangmeong.expense.dto.ExpenseResponse;
import com.nyangmeong.expense.dto.ExpenseSearchCond;
import com.nyangmeong.expense.dto.ExpenseStatisticsResponse;
import com.nyangmeong.expense.dto.ExpenseSummary;
import com.nyangmeong.expense.dto.ExpenseTotalResponse;
import com.nyangmeong.expense.dto.ExpenseUpdateRequest;
import com.nyangmeong.expense.service.ExpenseQueryService;
import com.nyangmeong.expense.service.ExpenseService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [가계부 지출 컨트롤러]
 * - 지출 등록·저장 전 중복 확인 (SCR-MYP-031)
 * - 월 통계·목록·합계·월 피커·필터 반려동물·수정·삭제 (SCR-MYP-030)
 * - 같은 멱등 요청 ID 의 재전송은 201 이 아니라 200 으로 같은 결과
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseQueryService expenseQueryService;

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

    @Operation(summary = "가계부 월 통계(총 지출·한도·달력·카테고리별·반려동물별·주요 지출)")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ExpenseStatisticsResponse>> getStatistics(
            @AuthenticationPrincipal LoginMember me, @RequestParam String yearMonth) {
        return ResponseEntity.ok(ApiResponse.success(expenseQueryService.getStatistics(me.id(), yearMonth)));
    }

    @Operation(summary = "가계부 지출 목록(기간·반려동물·공용·카테고리 필터, 최신순·금액순)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseSummary>>> search(@AuthenticationPrincipal LoginMember me,
            ExpenseSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(expenseQueryService.search(me.id(), cond, pq)));
    }

    @Operation(summary = "가계부 조건별 지출 건수·합계")
    @GetMapping("/totals")
    public ResponseEntity<ApiResponse<ExpenseTotalResponse>> getTotal(@AuthenticationPrincipal LoginMember me,
            ExpenseSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(expenseQueryService.getTotal(me.id(), cond)));
    }

    @Operation(summary = "가계부 지출이 있는 연월(월 피커)")
    @GetMapping("/months")
    public ResponseEntity<ApiResponse<List<ExpenseMonthSummary>>> searchMonths(
            @AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(expenseQueryService.searchMonths(me.id())));
    }

    @Operation(summary = "가계부 필터 반려동물(연동 해제·삭제된 반려동물 포함)")
    @GetMapping("/pets")
    public ResponseEntity<ApiResponse<List<ExpensePetOption>>> searchPets(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(expenseQueryService.searchPets(me.id())));
    }

    @Operation(summary = "가계부 지출 수정")
    @PatchMapping("/{expndSn}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long expndSn, @Valid @RequestBody ExpenseUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.update(me.id(), expndSn, req)));
    }

    @Operation(summary = "가계부 지출 한 건 삭제")
    @DeleteMapping("/{expndSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long expndSn) {
        expenseService.delete(me.id(), List.of(expndSn));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "가계부 지출 여러 건 삭제(개별 선택)")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAll(@AuthenticationPrincipal LoginMember me,
            @RequestParam("expndSn") List<Long> expndSns) {
        expenseService.delete(me.id(), expndSns);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "가계부 지출 조건 전체 삭제(조회 결과 전체 선택)")
    @PostMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ExpenseBulkDeleteRequest req) {
        expenseService.bulkDelete(me.id(), req);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
