package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [일상 건강기록 응답]
 * - 등록·수정 (목록 항목과 같은 모양)
 */
public record DailyHealthRecordResponse(
        Long dailyHlthRcdSn,
        String dailyRcdIdctCd,
        BigDecimal rcdNvl,
        String crsdSttsCd,
        String crsdSttsCdNm,
        LocalDateTime rcdDt) {

    public static DailyHealthRecordResponse from(DailyHealthRecordSummary summary) {
        return new DailyHealthRecordResponse(summary.getDailyHlthRcdSn(), summary.getDailyRcdIdctCd(),
                summary.getRcdNvl(), summary.getCrsdSttsCd(), summary.getCrsdSttsCdNm(), summary.getRcdDt());
    }
}
