package com.nyangmeong.global.log;

/**
 * [관리자 작업 유형]
 * - SYSG01 (MNGR_TASK_LOG.TASK_TYPE_CD)
 */
public enum AdminTaskType {
    CREATE("SYSC0001"),
    UPDATE("SYSC0002"),
    DELETE("SYSC0003"),
    PROCESS("SYSC0004"),
    LOGIN("SYSC0005");

    private final String code;

    AdminTaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
