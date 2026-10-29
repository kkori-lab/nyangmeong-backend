package com.nyangmeong.expense.dto;

/**
 * [월 지출 한도 저장 결과]
 * - expndLimNotiRcptnYn : 처음 저장하면 켜짐 (FN-MYP-030-13)
 */
public record ExpenseLimitResponse(Long mmExpndLimAmt, boolean expndLimNotiRcptnYn) {
}
