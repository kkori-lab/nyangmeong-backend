package com.nyangmeong.health.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * [검진기록 목록 검색 조건]
 * - year 가 없으면 연도 조건 없이 전체 (FN-HLT-002-14)
 */
@Data
public class CheckupSearchCond {

    /** 조회 연도 */
    @Min(1900)
    @Max(2200)
    private Integer year;
    /** 검진유형공통코드 */
    private String ckupTypeCd;
}
