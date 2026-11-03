package com.nyangmeong.expense.dto;

/**
 * [지출이 있는 연월]
 * - 월 피커 (FN-MYP-030-16), 형식 yyyy-MM
 */
public record ExpenseMonthSummary(String yearMonth) {
}
