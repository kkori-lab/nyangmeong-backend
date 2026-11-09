package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [건강기록 대시보드 항목 코드]
 * - HLTG25 (MBR_DSHBD_WDGT.DSHBD_WDGT_CD) : 선언 순서가 기본 순서
 */
public enum HealthDashboardItem {
    WEIGHT("HLTC0079"),
    MEAL("HLTC0080"),
    WATER("HLTC0081"),
    WALK("HLTC0082"),
    POOP("HLTC0083"),
    CKUP("HLTC0084"),
    INSP("HLTC0085"),
    PRESC("HLTC0086"),
    ATCH("HLTC0087"),
    OBSV("HLTC0088"),
    ALLERGY("HLTC0089"),
    VACC("HLTC0090"),
    CARE("HLTC0091");

    private final String code;

    HealthDashboardItem(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 항목 조회
     */
    public static Optional<HealthDashboardItem> of(String code) {
        return Arrays.stream(values()).filter(item -> item.is(code)).findFirst();
    }
}
