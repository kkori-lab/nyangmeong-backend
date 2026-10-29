package com.nyangmeong.expense.dto;

import java.time.LocalDate;

/**
 * [지출 등록·수정 결과]
 * - petSn 이 null 이면 공용
 */
public record ExpenseResponse(Long expndSn, Long petSn, String petNm, String expndClsfCd, Long expndAmt,
        String dtlMttr, String expndPlcusNm, LocalDate expndYmd, Long recptSn) {

    public static ExpenseResponse from(ExpenseSummary summary) {
        return new ExpenseResponse(summary.getExpndSn(), summary.getPetSn(), summary.getPetNm(),
                summary.getExpndClsfCd(), summary.getExpndAmt(), summary.getDtlMttr(), summary.getExpndPlcusNm(),
                summary.getExpndYmd(), summary.getRecptSn());
    }
}
