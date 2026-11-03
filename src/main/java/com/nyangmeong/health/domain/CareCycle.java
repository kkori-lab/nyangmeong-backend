package com.nyangmeong.health.domain;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

/**
 * [케어 주기 코드]
 * - HLTG15 (CARE_ARTCL.CARE_CYCLE_CD) : 직접입력은 일수(n일마다)를 따로 받음
 */
public enum CareCycle {
    WEEK_1("HLTC0038"),
    WEEK_2("HLTC0039"),
    MONTH_1("HLTC0040"),
    YEAR_1("HLTC0041"),
    CUSTOM("HLTC0042");

    private final String code;

    CareCycle(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 주기 조회
     */
    public static Optional<CareCycle> of(String code) {
        return Arrays.stream(values()).filter(cycle -> cycle.is(code)).findFirst();
    }

    /**
     * 기준일 다음 예정일 계산
     */
    public LocalDate next(LocalDate baseYmd, Integer drctInptDayCnt) {
        return switch (this) {
            case WEEK_1 -> baseYmd.plusWeeks(1);
            case WEEK_2 -> baseYmd.plusWeeks(2);
            case MONTH_1 -> baseYmd.plusMonths(1);
            case YEAR_1 -> baseYmd.plusYears(1);
            case CUSTOM -> baseYmd.plusDays(drctInptDayCnt);
        };
    }
}
