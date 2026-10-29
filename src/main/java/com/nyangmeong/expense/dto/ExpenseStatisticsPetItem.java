package com.nyangmeong.expense.dto;

import lombok.Data;

/**
 * [월 통계 - 반려동물별 합계] (FN-MYP-030-05)
 * - 반려동물 등록순, 공용 제외, 삭제된 반려동물도 포함
 */
@Data
public class ExpenseStatisticsPetItem {
    private Long petSn;
    private String petNm;
    private Boolean petDelYn;
    private Long totalExpndAmt;
}
