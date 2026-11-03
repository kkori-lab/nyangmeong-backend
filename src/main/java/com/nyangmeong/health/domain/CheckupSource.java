package com.nyangmeong.health.domain;

/**
 * [검진기록 출처 코드]
 * - HLTG03 (CKUP_RCD.RCD_SRC_CD)
 */
public enum CheckupSource {
    DIRECT("HLTC0008"),
    RECEIPT_OCR("HLTC0009");

    private final String code;

    CheckupSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
