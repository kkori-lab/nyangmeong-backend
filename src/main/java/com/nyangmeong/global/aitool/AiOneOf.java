package com.nyangmeong.global.aitool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [AI 매니저 도구 택일 칸]
 * - 예: 품종 번호·품종 직접 입력 중 하나만
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AiOneOf.List.class)
public @interface AiOneOf {

    /** 택일 칸 이름 */
    String[] value();

    /**
     * 반복 표시 묶음
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        AiOneOf[] value();
    }
}
