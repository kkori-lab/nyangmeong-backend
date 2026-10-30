package com.nyangmeong.community.domain;

/**
 * [게시판 코드]
 * - CMUG01 (PST.BBS_CD)
 * - LOST 는 실종센터 전용, 펫이야기 API 는 PET_INFO·PET_DAILY 만 받는다
 */
public enum BoardType {
    PET_INFO("CMUC0001"),
    PET_DAILY("CMUC0002"),
    LOST("CMUC0024");

    private final String code;

    BoardType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 펫이야기 게시판 코드 여부
     */
    public static boolean exists(String code) {
        for (BoardType type : values()) {
            if (type != LOST && type.is(code)) {
                return true;
            }
        }
        return false;
    }
}
