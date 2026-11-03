package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * [지출 수정 요청] (FN-MYP-030-03·12)
 * - null 인 필드는 그대로, dtlMttr·expndPlcusNm 은 "" 이면 비움
 * - common : true 면 공용으로 바꿈(petSn 과 함께 보낼 수 없음)
 */
public record ExpenseUpdateRequest(
        LocalDate expndYmd,
        String expndClsfCd,
        Long petSn,
        Boolean common,
        @Min(value = 1, message = "금액은 1원 이상이어야 해요") @Max(9_999_999_999_999L) Long expndAmt,
        @Size(max = 1000) String dtlMttr,
        @Size(max = 80) String expndPlcusNm) {
}
