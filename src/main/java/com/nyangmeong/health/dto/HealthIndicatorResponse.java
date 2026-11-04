package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [메인 건강 지표 응답]
 * - 기준일·조회 시각과 위젯 5종 값. 기록이 없는 지표는 null (FN-MAN-005·006)
 */
public record HealthIndicatorResponse(
        LocalDate baseYmd,
        LocalDateTime inqDt,
        WeightIndicator weight,
        DailySumIndicator meal,
        DailySumIndicator water,
        DailySumIndicator walk,
        CreatinineIndicator creatinine) {

    /** 체중: 최신값, 직전 기록 대비, 최근 5건(오래된 순) */
    public record WeightIndicator(BigDecimal latestNvl, BigDecimal prevDiff, List<HealthWeightPoint> points) {
    }

    /** 식사량·음수량·산책: 오늘 일 합계, 직전 기록일 일 합계, 대비, 기록 있는 최근 5일(오래된 순) */
    public record DailySumIndicator(BigDecimal todaySum, BigDecimal prevDaySum, BigDecimal prevDiff,
            List<HealthDailySumPoint> points) {
    }

    /** 혈액검사(CREA): 최신 수치, 지난 검사 대비, 최근 5회(오래된 순) */
    public record CreatinineIndicator(BigDecimal latestNvl, String unitNm, BigDecimal refLlmtNvl,
            BigDecimal refUplmtNvl, BigDecimal prevDiff, List<HealthCreatininePoint> points) {
    }
}
