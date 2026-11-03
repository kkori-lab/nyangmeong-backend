package com.nyangmeong.expense.dto;

import lombok.Data;

/**
 * [월 통계 - 카테고리별 합계] (FN-MYP-030-04)
 * - 금액 큰 순, 기타는 마지막
 */
@Data
public class ExpenseStatisticsCategoryItem {
    private String expndClsfCd;
    private Long totalExpndAmt;
}
