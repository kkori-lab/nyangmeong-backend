package com.nyangmeong.place.domain;

/**
 * [장소 출처]
 * - PLCG02 (PLC.PLC_SRC_CD)
 */
public enum PlaceSource {
    PUBLIC_DATA("PLCC0006"),
    ADMIN("PLCC0007"),
    KAKAO("PLCC0008");

    private final String code;

    PlaceSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
