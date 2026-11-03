package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [지출 목록 항목]
 * - 목록(SCR-MYP-030)·중복 지출 확인 모달(FN-MYP-031-06)
 * - petDelYn : 공용이면 null
 */
@Data
public class ExpenseSummary {
    private Long expndSn;
    private LocalDate expndYmd;
    private String expndClsfCd;
    private Long petSn;
    private String petNm;
    private Boolean petDelYn;
    private String dtlMttr;
    private String expndPlcusNm;
    private Long expndAmt;
    private Long recptSn;
}
