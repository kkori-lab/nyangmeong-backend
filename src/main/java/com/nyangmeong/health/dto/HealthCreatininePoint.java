package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [메인 건강 지표 - 크레아티닌 점]
 * - 혈액검사 위젯의 검진별 CREA 수치 (FN-MAN-006)
 */
@Data
public class HealthCreatininePoint {

    /** 검진 일정일시 */
    private LocalDateTime schdlDt;
    /** 결과수치 */
    private BigDecimal rsltNvl;
    /** 단위명 */
    private String unitNm;
    /** 참고하한수치 */
    private BigDecimal refLlmtNvl;
    /** 참고상한수치 */
    private BigDecimal refUplmtNvl;
}
