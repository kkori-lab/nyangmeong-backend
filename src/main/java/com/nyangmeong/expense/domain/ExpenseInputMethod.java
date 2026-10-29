package com.nyangmeong.expense.domain;

import java.util.Arrays;

/**
 * [가계부 입력방식 코드]
 * - MYPG06 (EXPND.EXPND_INPT_MTH_CD)
 * - 영수증 인식 실패 뒤 직접 입력은 DIRECT + 영수증 연결
 */
public enum ExpenseInputMethod {
    DIRECT("MYPC0019"),
    OCR("MYPC0020");

    private final String code;

    ExpenseInputMethod(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드 존재 여부
     */
    public static boolean exists(String code) {
        return Arrays.stream(values()).anyMatch(method -> method.is(code));
    }
}
