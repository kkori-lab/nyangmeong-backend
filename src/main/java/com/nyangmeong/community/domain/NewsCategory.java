package com.nyangmeong.community.domain;

/**
 * [뉴스 카테고리 코드]
 * - CMUG06 (NEWS.NEWS_CLSF_CD)
 */
public enum NewsCategory {
    HEALTH("CMUC0018"),
    LIFE("CMUC0019"),
    POLICY("CMUC0020"),
    TRAVEL("CMUC0021");

    private final String code;

    NewsCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 허용 코드 여부
     */
    public static boolean exists(String code) {
        for (NewsCategory category : values()) {
            if (category.is(code)) {
                return true;
            }
        }
        return false;
    }
}
