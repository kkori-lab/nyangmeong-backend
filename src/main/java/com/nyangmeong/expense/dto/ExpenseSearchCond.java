package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [지출 검색 조건]
 * - 목록·합계·조건 삭제가 같이 씀 (FN-MYP-030-09·10·17)
 * - 기간을 비우면 전체 기간, petSn 과 common 을 비우면 반려동물 전체
 * - common : true 면 공용(PET_SN 없음)만
 */
@Data
public class ExpenseSearchCond {
    private LocalDate startYmd;
    private LocalDate endYmd;
    private Long petSn;
    private Boolean common;
    private String expndClsfCd;
}
