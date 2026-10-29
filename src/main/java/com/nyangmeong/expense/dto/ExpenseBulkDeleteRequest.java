package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * [지출 조건 전체 삭제 요청] (FN-MYP-030-18·19·21)
 * - 조회 결과 전체 선택 : 범위·필터에 맞는 전체 건
 * - expectedCnt : 화면이 보여 준 건수, 서버가 센 건수와 다르면 400
 */
public record ExpenseBulkDeleteRequest(
        @NotNull @Min(1) Integer expectedCnt,
        LocalDate startYmd,
        LocalDate endYmd,
        Long petSn,
        Boolean common,
        String expndClsfCd) {

    public ExpenseSearchCond toCond() {
        ExpenseSearchCond cond = new ExpenseSearchCond();
        cond.setStartYmd(startYmd);
        cond.setEndYmd(endYmd);
        cond.setPetSn(petSn);
        cond.setCommon(common);
        cond.setExpndClsfCd(expndClsfCd);
        return cond;
    }
}
