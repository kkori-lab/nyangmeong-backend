package com.nyangmeong.community.domain;

/**
 * [신고 조치 코드]
 * - CMUG08 (DCLR.DCLR_ACTN_CD)
 */
public enum ReportAction {
    KEEP("CMUC0025"),
    BLIND("CMUC0026"),
    SUSPEND("CMUC0027");

    private final String code;

    ReportAction(String code) {
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
        for (ReportAction action : values()) {
            if (action.is(code)) {
                return true;
            }
        }
        return false;
    }
}
