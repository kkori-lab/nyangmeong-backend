package com.nyangmeong.community.domain;

/**
 * [펫돌봄 매칭 거래 상태 코드]
 * - PCAG05 (PET_PRTC_MTCH.MTCH_DLNG_STTS_CD), 「매칭 전」은 매칭 행이 없는 상태
 */
public enum CareMatchStatus {
    REQUESTED("PCAC0012"),
    MATCHED("PCAC0013"),
    AWAITING_CONFIRM("PCAC0014"),
    AWAITING_RECEIPT("PCAC0015"),
    COMPLETED("PCAC0016"),
    CANCELED("PCAC0017"),
    NO_RESPONSE("PCAC0018");

    private final String code;

    CareMatchStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 수락 대기 ~ 수령 대기 : 나가면 요청이 취소되는 진행 중 상태
     */
    public static boolean isInProgress(String code) {
        return REQUESTED.is(code) || MATCHED.is(code) || AWAITING_CONFIRM.is(code) || AWAITING_RECEIPT.is(code);
    }

    /**
     * 모집글이 매칭완료로 묶여 있는 상태(매칭 완료 ~ 수령 대기)
     */
    public static boolean isConfirmed(String code) {
        return MATCHED.is(code) || AWAITING_CONFIRM.is(code) || AWAITING_RECEIPT.is(code);
    }
}
