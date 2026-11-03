package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [관찰 회차 응답]
 * - 회차 추가·수정 (목록 행과 같은 모양)
 */
public record ObservationCycleResponse(
        Long obsrvCyclSn,
        Long obsrvArtclSn,
        Integer cyclNo,
        LocalDateTime obsrvDt,
        BigDecimal obsrvSz,
        String obsrvChgCd,
        String obsrvChgCdNm,
        String memoCn) {

    public static ObservationCycleResponse from(ObservationCycleSummary summary) {
        return new ObservationCycleResponse(summary.getObsrvCyclSn(), summary.getObsrvArtclSn(),
                summary.getCyclNo(), summary.getObsrvDt(), summary.getObsrvSz(), summary.getObsrvChgCd(),
                summary.getObsrvChgCdNm(), summary.getMemoCn());
    }
}
