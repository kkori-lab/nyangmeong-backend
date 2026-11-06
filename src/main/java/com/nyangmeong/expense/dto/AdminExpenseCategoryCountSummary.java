package com.nyangmeong.expense.dto;

import lombok.Data;

/**
 * [관리자 대시보드 가계부 카테고리별 지출 건수]
 * - SCR-ADM-001 (FN-ADM-001-04) : 카테고리 전부, 건이 없으면 0
 */
@Data
public class AdminExpenseCategoryCountSummary {
    /** 가계부카테고리공통코드(MYPG05) */
    private String ctgryCd;
    /** 카테고리 이름 */
    private String ctgryNm;
    /** 지출 건수 */
    private long cnt;
}
