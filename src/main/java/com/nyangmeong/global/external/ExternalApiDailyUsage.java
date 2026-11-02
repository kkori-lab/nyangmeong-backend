package com.nyangmeong.global.external;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * [외부 API 일별 사용량 응답]
 */
@Data
public class ExternalApiDailyUsage {
    /** 호출일자 */
    private LocalDate callYmd;
    /** 외부API구분공통코드 */
    private String extlApiSeCd;
    /** 호출 수 */
    private long callCnt;
    /** 실패·시간초과 수 */
    private long failCnt;
    /** 평균 응답 밀리초, 응답 시간이 있는 호출만 */
    private Long avgRspnsMs;

    public String getExtlApiNm() {
        return ExternalApiType.labelOf(extlApiSeCd);
    }

    public BigDecimal getFailRate() {
        return ExternalApiUsageSummary.failRate(failCnt, callCnt);
    }
}
