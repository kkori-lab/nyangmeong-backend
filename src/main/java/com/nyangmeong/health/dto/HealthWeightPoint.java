package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [메인 건강 지표 - 체중 점]
 * - 체중 위젯 최근 기록 (FN-MAN-006)
 */
@Data
public class HealthWeightPoint {

    /** 기록일시 */
    private LocalDateTime rcdDt;
    /** 기록수치 */
    private BigDecimal rcdNvl;
}
