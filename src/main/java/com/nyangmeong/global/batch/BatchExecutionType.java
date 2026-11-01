package com.nyangmeong.global.batch;

/**
 * [배치 실행 구분]
 * - SYSG03 (BTCH_EXCN_HSTRY.EXCN_SE_CD)
 */
public enum BatchExecutionType {
    AUTO("SYSC0010"),
    MANUAL("SYSC0011");

    private final String code;

    BatchExecutionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
