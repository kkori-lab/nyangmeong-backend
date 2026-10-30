package com.nyangmeong.community.domain;

/**
 * [실종제보 상태 코드]
 * - CMUG03 (LOST_TPOF.LOST_TPOF_STTS_CD), 찾았어요는 취소 불가
 */
public enum LostReportStatus {
    MISSING("CMUC0006"),
    FOUND("CMUC0007"),
    EXPIRED("CMUC0008");

    private final String code;

    LostReportStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
