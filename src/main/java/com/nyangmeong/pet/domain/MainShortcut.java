package com.nyangmeong.pet.domain;

/**
 * [메인 바로가기 종류]
 * - 공통코드 MANG02, 이동 경로는 프론트가 코드로 고름
 */
public enum MainShortcut {
    HEALTH_RECORD("MANC0008"),
    HEALTH_DIARY("MANC0009"),
    CARE_CALENDAR("MANC0010");

    private final String code;

    MainShortcut(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
