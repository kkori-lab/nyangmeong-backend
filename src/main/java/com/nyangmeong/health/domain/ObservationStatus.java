package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [관찰 상태]
 * - HLTG18 (OBSRV_ARTCL.OBSRV_STTS_CD)
 */
public enum ObservationStatus {
    ONGOING("HLTC0055"),
    ENDED("HLTC0056");

    private final String code;

    ObservationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 코드로 상태 조회
     */
    public static Optional<ObservationStatus> of(String code) {
        return Arrays.stream(values()).filter(status -> status.code.equals(code)).findFirst();
    }
}
