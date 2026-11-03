package com.nyangmeong.global.external;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Data;

/**
 * [외부 API 종류별 사용량 응답]
 */
@Data
public class ExternalApiUsageSummary {
    /** 외부API구분공통코드 */
    private String extlApiSeCd;
    /** 호출 수 */
    private long callCnt;
    /** 실패·시간초과 수 */
    private long failCnt;
    /** 평균 응답 밀리초, 응답 시간이 있는 호출만 */
    private Long avgRspnsMs;

    /**
     * 호출이 없는 종류의 사용량
     */
    public static ExternalApiUsageSummary empty(ExternalApiType type) {
        ExternalApiUsageSummary summary = new ExternalApiUsageSummary();
        summary.setExtlApiSeCd(type.getCode());
        return summary;
    }

    public String getExtlApiNm() {
        return ExternalApiType.labelOf(extlApiSeCd);
    }

    public BigDecimal getFailRate() {
        return failRate(failCnt, callCnt);
    }

    /**
     * 실패율(%), 소수 첫째 자리
     */
    static BigDecimal failRate(long failCnt, long callCnt) {
        if (callCnt == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(failCnt * 100).divide(BigDecimal.valueOf(callCnt), 1, RoundingMode.HALF_UP);
    }
}
