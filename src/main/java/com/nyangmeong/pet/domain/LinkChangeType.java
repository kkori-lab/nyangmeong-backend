package com.nyangmeong.pet.domain;

/**
 * [연동 변경 유형 코드]
 * - PETG07 (BRUP_LINK_STTS_HSTRY.LINK_CHG_TYPE_CD)
 */
public enum LinkChangeType {
    INVITATION_ACCEPTED("PETC0024"),
    SELF_UNLINKED("PETC0025"),
    FORCED_UNLINKED("PETC0026"),
    PET_DELETED("PETC0027");

    private final String code;

    LinkChangeType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
