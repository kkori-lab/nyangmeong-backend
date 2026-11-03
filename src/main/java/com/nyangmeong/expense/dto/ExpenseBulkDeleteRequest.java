package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [지출 조건 전체 삭제 요청] (FN-MYP-030-18·19·21)
 * - 조회 결과 전체 선택 : 범위·필터에 맞는 전체 건
 * - excludeExpndSn : 전체 선택 뒤 개별 해제한 지출 번호, 빼고 지움
 * - expectedCnt : 화면이 보여 준 건수(조건 건수 − 제외 수), 서버가 센 값과 다르면 409
 */
public record ExpenseBulkDeleteRequest(
        @NotNull @Min(1) Integer expectedCnt,
        LocalDate startYmd,
        LocalDate endYmd,
        Long petSn,
        Boolean common,
        String expndClsfCd,
        @Size(max = 1000) List<@NotNull Long> excludeExpndSn) {

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
