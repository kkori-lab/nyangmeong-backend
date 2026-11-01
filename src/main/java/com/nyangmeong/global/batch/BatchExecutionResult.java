package com.nyangmeong.global.batch;

/**
 * [배치 실행 결과]
 * - SYSG04 (BTCH_EXCN_HSTRY.EXCN_RSLT_CD)
 */
public enum BatchExecutionResult {
    RUNNING("SYSC0012"),
    SUCCESS("SYSC0013"),
    FAIL("SYSC0014");

    private final String code;

    BatchExecutionResult(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
