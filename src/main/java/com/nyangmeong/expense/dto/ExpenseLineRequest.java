package com.nyangmeong.expense.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [지출 등록 반려동물·금액 줄]
 * - petSn 이 null 이면 공용 (FN-MYP-031-02·03)
 * - idmptDmndId : 줄마다 다른 값, 중복 클릭·재전송 방지
 */
public record ExpenseLineRequest(
        Long petSn,
        @NotNull @Min(value = 1, message = "금액은 1원 이상이어야 해요") @Max(9_999_999_999_999L) Long expndAmt,
        @NotBlank @Size(max = 100) String idmptDmndId) {
}
