package com.nyangmeong.expense.domain;

import java.util.Arrays;

/**
 * [가계부 카테고리 코드]
 * - MYPG05 (EXPND.EXPND_CLSF_CD), 7종 고정
 */
public enum ExpenseCategory {
    HOSPITAL("MYPC0012"),
    FOOD("MYPC0013"),
    SNACK("MYPC0014"),
    SUPPLIES("MYPC0015"),
    GROOMING("MYPC0016"),
    CARE("MYPC0017"),
    ETC("MYPC0018");

    private final String code;

    ExpenseCategory(String code) {
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
        return Arrays.stream(values()).anyMatch(category -> category.is(code));
    }
}
