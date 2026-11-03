package com.nyangmeong.expense.domain;

/**
 * [영수증 OCR 요청 출처 코드]
 * - AIMG07 (RECPT.OCR_DMND_SRC_CD)
 */
public enum ReceiptSource {
    HEALTH("AIMC0026"),
    EXPENSE("AIMC0027"),
    CHAT("AIMC0031");

    private final String code;

    ReceiptSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 출처 조회 : 없거나 모르는 코드는 건강 일지 (출처를 주지 않은 업로드의 기본값)
     */
    public static ReceiptSource from(String code) {
        for (ReceiptSource source : values()) {
            if (source.is(code)) {
                return source;
            }
        }
        return HEALTH;
    }
}
