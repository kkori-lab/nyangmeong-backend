package com.nyangmeong.community.domain;

/**
 * [펫돌봄 모집 상태 코드]
 * - PCAG01 (PET_PRTC_RCRT.RCRT_STTS_CD)
 */
public enum CareRecruitmentStatus {
    RECRUITING("PCAC0001"),
    MATCHED("PCAC0002"),
    COMPLETED("PCAC0003");

    private final String code;

    CareRecruitmentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 목록 필터로 받을 수 있는 상태 코드 여부
     */
    public static boolean exists(String code) {
        for (CareRecruitmentStatus status : values()) {
            if (status.is(code)) {
                return true;
            }
        }
        return false;
    }
}
