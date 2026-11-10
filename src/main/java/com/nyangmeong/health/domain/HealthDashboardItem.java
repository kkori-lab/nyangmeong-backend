package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [건강기록 대시보드 항목 코드]
 * - HLTG25 (MBR_DSHBD_WDGT.DSHBD_WDGT_CD) : 선언 순서가 기본 순서
 */
public enum HealthDashboardItem {
    WEIGHT("HLTC0084"),
    MEAL("HLTC0085"),
    WATER("HLTC0086"),
    WALK("HLTC0087"),
    POOP("HLTC0088"),
    CKUP("HLTC0089"),
    INSP("HLTC0090"),
    PRESC("HLTC0091"),
    ATCH("HLTC0092"),
    OBSV("HLTC0093"),
    ALLERGY("HLTC0094"),
    VACC("HLTC0095"),
    CARE("HLTC0096");

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
