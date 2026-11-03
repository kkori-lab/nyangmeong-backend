package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [처방 상태 코드]
 * - HLTG06 (PRSC.PRSC_STTS_CD)
 */
public enum PrescriptionStatus {
    TAKING("HLTC0010"),
    ENDED("HLTC0011");

    private final String code;

    PrescriptionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 상태 조회
     */
    public static Optional<PrescriptionStatus> of(String code) {
        return Arrays.stream(values()).filter(status -> status.is(code)).findFirst();
    }
}
