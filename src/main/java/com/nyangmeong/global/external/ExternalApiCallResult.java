package com.nyangmeong.global.external;

import java.util.Arrays;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [외부 API 호출 결과]
 * - SYSG06 (EXTL_API_CALL_LOG.CALL_RSLT_CD)
 */
public enum ExternalApiCallResult {
    SUCCESS("SYSC0021"),
    FAIL("SYSC0022"),
    TIMEOUT("SYSC0023");

    private final String code;

    ExternalApiCallResult(String code) {
        this.code = code;
    }

    /**
     * 코드로 호출 결과 조회
     */
    public static ExternalApiCallResult from(String code) {
        return Arrays.stream(values())
                .filter(result -> result.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }

    public String getCode() {
        return code;
    }
}
