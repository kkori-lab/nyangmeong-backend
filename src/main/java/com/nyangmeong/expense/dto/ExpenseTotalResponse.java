package com.nyangmeong.expense.dto;

/**
 * [조건별 지출 건수·합계]
 * - 전체 내역 월 합계(FN-MYP-030-09), 조회 결과 전체 선택 요약(FN-MYP-030-18·19)
 */
public record ExpenseTotalResponse(Integer expndCnt, Long totalExpndAmt) {
}
