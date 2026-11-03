package com.nyangmeong.community.domain;

/**
 * [펫돌봄 형태 코드]
 * - PCAG03 (RCRT_PRTC_SHP.PRTC_SHP_CD)
 */
public enum CareShape {
    VISIT("PCAC0006"),
    BOARDING("PCAC0007"),
    WALK("PCAC0008"),
    PLAY("PCAC0009"),
    TRAINING("PCAC0010"),
    ETC("PCAC0011");

    private final String code;

    CareShape(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    public static boolean exists(String code) {
        for (CareShape shape : values()) {
            if (shape.is(code)) {
                return true;
            }
        }
        return false;
    }
}
