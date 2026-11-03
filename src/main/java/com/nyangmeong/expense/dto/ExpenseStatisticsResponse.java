package com.nyangmeong.expense.dto;

import java.util.List;

/**
 * [가계부 월 통계] (FN-MYP-030-01·02·04·05·06·14)
 * - mmExpndLimAmt 가 null 이면 한도 미설정
 * - lastMonthTotalExpndAmt : 이 달 이전 지출이 한 건도 없으면(첫 달) null
 * - 공용 지출은 pets 에 넣지 않고 commonTotalExpndAmt 로 따로
 */
public record ExpenseStatisticsResponse(
        Long totalExpndAmt,
        Long mmExpndLimAmt,
        Long lastMonthTotalExpndAmt,
        List<ExpenseStatisticsDayItem> days,
        List<ExpenseStatisticsCategoryItem> categories,
        List<ExpenseStatisticsPetItem> pets,
        Long commonTotalExpndAmt,
        List<ExpenseSummary> topExpenses) {
}
