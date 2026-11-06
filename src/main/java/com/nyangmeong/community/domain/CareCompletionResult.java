package com.nyangmeong.community.domain;

/**
 * [보호자 확인 결과 코드]
 * - PCAG06 (PRTC_CMPTN_DMND_CYCL.PRTCR_IDNTY_RSLT_CD)
 */
public enum CareCompletionResult {
    CONFIRMED("PCAC0019"),
    SUPPLEMENT_REQUESTED("PCAC0020");

    private final String code;

    CareCompletionResult(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
