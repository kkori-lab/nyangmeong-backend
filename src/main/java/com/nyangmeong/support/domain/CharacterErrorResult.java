package com.nyangmeong.support.domain;

/**
 * [캐릭터 오류 처리 결과 코드]
 * - CHRG04 (OTO_INQRY.CHAR_ERR_PRCS_RSLT_CD)
 */
public enum CharacterErrorResult {
    REGENERATE("CHRC0008"),
    RESTORE("CHRC0009");

    private final String code;

    CharacterErrorResult(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 결과 찾기 (없으면 null)
     */
    public static CharacterErrorResult from(String code) {
        for (CharacterErrorResult result : values()) {
            if (result.is(code)) {
                return result;
            }
        }
        return null;
    }
}
