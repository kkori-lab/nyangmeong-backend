package com.nyangmeong.health.domain;

/**
 * [검진기록 상태 코드]
 * - HLTG02 (CKUP_RCD.CKUP_RCD_STTS_CD)
 */
public enum CheckupStatus {
    COMPLETED("HLTC0006"),
    DELETED("HLTC0007");

    private final String code;

    CheckupStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
