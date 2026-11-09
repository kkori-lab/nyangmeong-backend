package com.nyangmeong.health.domain;

/**
 * [검진 유형 코드]
 * - HLTG01 (CKUP_RCD.CKUP_TYPE_CD, SCHDL.CKUP_TYPE_CD)
 */
public enum CheckupType {
    REGULAR("HLTC0001"),
    VACCINATION("HLTC0002"),
    DISEASE("HLTC0003"),
    TEST("HLTC0004"),
    ETC("HLTC0005");

    private final String code;

    CheckupType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
