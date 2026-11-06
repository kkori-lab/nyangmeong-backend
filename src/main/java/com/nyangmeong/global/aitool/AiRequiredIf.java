package com.nyangmeong.global.aitool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [AI 매니저 도구 조건부 필수]
 * - when 칸이 equals 값이면 field 칸 필수
 * - 값이 여럿이면 값마다 한 번씩 붙임
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AiRequiredIf.List.class)
public @interface AiRequiredIf {

    /** 필수가 되는 칸 */
    String field();

    /** 조건 칸 */
    String when();

    /** 조건 값 */
    String equals();

    /**
     * 반복 표시 묶음
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        AiRequiredIf[] value();
    }
}
