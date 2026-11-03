package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [월 통계 - 날짜별 합계] (FN-MYP-030-02)
 * - 반려동물·카테고리는 그날 1건일 때만, 공용이면 petSn·petNm·petDelYn 이 null
 */
@Data
public class ExpenseStatisticsDayItem {
    private LocalDate expndYmd;
    private Long totalExpndAmt;
    private Integer expndCnt;
    private Long petSn;
    private String petNm;
    private Boolean petDelYn;
    private String expndClsfCd;
}
