package com.nyangmeong.expense.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * [월 지출 한도 저장 요청] (FN-MYP-030-13)
 * - 계정 전체 한도 하나
 */
public record ExpenseLimitSaveRequest(
        @NotNull @Min(value = 1, message = "한도는 1원 이상이어야 해요") @Max(9_999_999_999_999L) Long mmExpndLimAmt) {
}
