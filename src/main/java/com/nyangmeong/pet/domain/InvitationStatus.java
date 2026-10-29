package com.nyangmeong.pet.domain;

/**
 * [초대 상태 코드]
 * - PETG06 (JNT_BRUP_INVTN.INVTN_STTS_CD)
 * - 만료 : 대기중이어도 만료일시가 지나면 만료로 봄 (배치 없음)
 */
public enum InvitationStatus {
    PENDING("PETC0020"),
    ACCEPTED("PETC0021"),
    REJECTED("PETC0022"),
    EXPIRED("PETC0023");

    private final String code;

    InvitationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
