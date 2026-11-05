package com.nyangmeong.ai.domain;

import java.util.Arrays;

/**
 * [이동 방식 코드]
 * - AIMG09 (AI_ROUT_MPNG.MVMN_MTHD_CD)
 * - 화면 이동·모달 열기만 실제로 이동하는 행이라 화면 ID와 지시어가 있어야 한다
 */
public enum AiRouteMoveType {
    PAGE("AIMC0032", true),
    MODAL("AIMC0033", true),
    NOT_ROUTABLE("AIMC0034", false),
    EXCLUDED("AIMC0035", false),
    PENDING("AIMC0036", false);

    private final String code;
    private final boolean routable;

    AiRouteMoveType(String code, boolean routable) {
        this.code = code;
        this.routable = routable;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    public boolean isRoutable() {
        return routable;
    }

    /**
     * 코드에 맞는 이동 방식, 없으면 null
     */
    public static AiRouteMoveType from(String code) {
        return Arrays.stream(values()).filter(t -> t.is(code)).findFirst().orElse(null);
    }
}
