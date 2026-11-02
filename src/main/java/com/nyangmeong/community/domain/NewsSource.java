package com.nyangmeong.community.domain;

/**
 * [뉴스 출처 코드]
 * - CMUG07 (NEWS.NEWS_SRC_CD)
 */
public enum NewsSource {
    PRESS("CMUC0022"),
    ADMIN("CMUC0023");

    private final String code;

    NewsSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
