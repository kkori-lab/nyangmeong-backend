package com.nyangmeong.global.external;

/**
 * [SNS 제공자]
 * - code : 공통코드 USRG03 (MBR.JOIN_PATH_CD, SNS_ACNT_LINK.SNS_OFFR_CD)
 */
public enum SnsProvider {
    NAVER("USRC0007"),
    KAKAO("USRC0008"),
    GOOGLE("USRC0009");

    private final String code;

    SnsProvider(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 공통코드로 찾기, 이메일(USRC0006)이나 모르는 값이면 null
     */
    public static SnsProvider fromCode(String code) {
        for (SnsProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        return null;
    }
}
