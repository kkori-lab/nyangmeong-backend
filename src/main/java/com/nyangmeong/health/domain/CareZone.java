package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [케어 구역 코드]
 * - HLTG13 (CARE_ARTCL.CARE_ZONE_CD) : 일상관리 = 매일, 정기관리 = 주기
 */
public enum CareZone {
    DAILY("HLTC0036"),
    REGULAR("HLTC0037");

    private final String code;

    CareZone(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 구역 조회
     */
    public static Optional<CareZone> of(String code) {
        return Arrays.stream(values()).filter(zone -> zone.is(code)).findFirst();
    }
}
