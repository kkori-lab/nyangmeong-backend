package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [첨부 유형 코드]
 * - HLTG20 (CKUP_RCD_ATCH.ATCH_TYPE_CD) : 영상 사진은 영상 종류·촬영 부위 필수
 */
public enum AttachmentType {
    INSPECTION_RESULT("HLTC0061"),
    VACCINATION_CERTIFICATE("HLTC0062"),
    IMAGE("HLTC0064");

    private final String code;

    AttachmentType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 유형 조회
     */
    public static Optional<AttachmentType> of(String code) {
        return Arrays.stream(values()).filter(type -> type.is(code)).findFirst();
    }
}
