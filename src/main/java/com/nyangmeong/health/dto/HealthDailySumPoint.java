package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * [메인 건강 지표 - 일 합계 점]
 * - 식사량·음수량·산책 위젯의 기록 있는 날 일 합계 (FN-MAN-006)
 */
@Data
public class HealthDailySumPoint {

    /** 기록일 */
    private LocalDate rcdYmd;
    /** 일 합계 */
    private BigDecimal sumNvl;
}
