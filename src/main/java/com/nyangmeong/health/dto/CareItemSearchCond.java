package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [케어 항목 검색 조건]
 * - careArtclNm : 같은 이름 항목 확인 (FN-AIM-012-08)
 */
@Data
public class CareItemSearchCond {

    /** 메인노출여부 */
    private Boolean mainExpsrYn;
    /** 케어항목명 */
    private String careArtclNm;
}
