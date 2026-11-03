package com.nyangmeong.expense.domain;

import java.util.Arrays;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [지출 목록 정렬]
 * - latest : 최신순(기본), amount : 금액 큰 순 (FN-MYP-030-11)
 */
public enum ExpenseSort {
    LATEST("latest"),
    AMOUNT("amount");

    private final String code;

    ExpenseSort(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 요청값으로 정렬 찾기 : 비우면 최신순, 없는 값이면 400
     */
    public static ExpenseSort from(String code) {
        if (code == null || code.isBlank()) {
            return LATEST;
        }
        return Arrays.stream(values())
                .filter(sort -> sort.is(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }
}
