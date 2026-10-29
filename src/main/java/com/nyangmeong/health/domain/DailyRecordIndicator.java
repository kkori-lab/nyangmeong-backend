package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [일상기록 지표]
 * - HLTG09 (DAILY_HLTH_RCD.DAILY_RCD_IDCT_CD) : 배변상태만 상태 코드(HLTG10), 나머지는 수치
 */
public enum DailyRecordIndicator {
    WEIGHT("HLTC0014"),
    MEAL("HLTC0015"),
    WATER("HLTC0016"),
    STOOL("HLTC0017"),
    WALK("HLTC0018");

    private final String code;

    DailyRecordIndicator(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 배변상태 여부 (상태 코드를 쓰는 지표)
     */
    public boolean isStool() {
        return this == STOOL;
    }

    /**
     * 코드로 지표 조회
     */
    public static Optional<DailyRecordIndicator> of(String code) {
        return Arrays.stream(values()).filter(indicator -> indicator.code.equals(code)).findFirst();
    }
}
