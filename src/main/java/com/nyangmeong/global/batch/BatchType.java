package com.nyangmeong.global.batch;

import java.util.Arrays;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [배치 구분]
 * - SYSG02 (BTCH_EXCN_HSTRY.BTCH_SE_CD)
 */
public enum BatchType {
    CARE_MISSION("SYSC0006", "케어미션 출제"),
    CARE_SCHEDULE_ALARM("SYSC0007", "케어 일정 사전 알림"),
    AI_CONVERSATION_PURGE("SYSC0008", "AI 대화 정리"),
    PRESCRIPTION_EXPIRY("SYSC0009", "처방 만료");

    private final String code;
    private final String label;

    BatchType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 코드로 배치 구분 조회
     */
    public static BatchType from(String code) {
        return Arrays.stream(values())
                .filter(type -> type.is(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
