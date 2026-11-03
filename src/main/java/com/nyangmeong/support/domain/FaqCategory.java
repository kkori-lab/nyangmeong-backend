package com.nyangmeong.support.domain;

/**
 * [FAQ 카테고리 코드]
 * - CSCG01 (FAQ.FAQ_CLSF_CD)
 * - 카테고리는 db 기준 파일 SQL 로 바꾼다. 코드를 바꾸면 이 enum 도 같이 고친다 (FN-ADM-041-04)
 */
public enum FaqCategory {
    USAGE("CSCC0001"),
    ACCOUNT("CSCC0002"),
    REPORT_SANCTION("CSCC0003"),
    ETC("CSCC0004");

    private final String code;

    FaqCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 허용 코드 여부
     */
    public static boolean exists(String code) {
        for (FaqCategory category : values()) {
            if (category.is(code)) {
                return true;
            }
        }
        return false;
    }
}
