package com.nyangmeong.expense.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [가계부 지출 VO]
 * - EXPND 1:1
 */
@Data
public class Expense {
    private Long expndSn;
    private Long petSn;
    private Long mbrSn;
    private Long recptSn;
    private String expndClsfCd;
    private String expndInptMthCd;
    private Long expndAmt;
    private String expndPlcusNm;
    private String dtlMttr;
    private LocalDate expndYmd;
    private String idmptDmndId;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
