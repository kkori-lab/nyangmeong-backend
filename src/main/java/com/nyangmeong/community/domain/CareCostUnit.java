package com.nyangmeong.community.domain;

/**
 * [펫돌봄 비용 단위 코드]
 * - PCAG02 (PET_PRTC_RCRT.CST_UNIT_CD)
 */
public enum CareCostUnit {
    HOURLY("PCAC0004"),
    PER_CASE("PCAC0005");

    private final String code;

    CareCostUnit(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    public static boolean exists(String code) {
        for (CareCostUnit unit : values()) {
            if (unit.is(code)) {
                return true;
            }
        }
        return false;
    }
}
