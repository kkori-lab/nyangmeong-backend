package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [지출 등록 요청]
 * - 날짜·카테고리·사용처·내용·영수증은 모든 줄에 공통 (FN-MYP-031-03)
 * - 중복 확인(POST /expenses/duplicate-check)도 같은 본문
 */
public record ExpenseCreateRequest(
        @NotNull LocalDate expndYmd,
        @NotBlank String expndClsfCd,
        @Size(max = 1000) String dtlMttr,
        @Size(max = 80) String expndPlcusNm,
        Long recptSn,
        @NotBlank String expndInptMthCd,
        @NotEmpty(message = "반려동물과 금액을 1줄 이상 입력해 주세요") @Valid List<ExpenseLineRequest> lines) {

    public String dtlMttrOrNull() {
        return blankToNull(dtlMttr);
    }

    public String expndPlcusNmOrNull() {
        return blankToNull(expndPlcusNm);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
