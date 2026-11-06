package com.nyangmeong.global.aitool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [AI 매니저 도구 칸 - 실행하는 쪽이 만드는 값]
 * - 중복 방지 키 등 LLM이 채우지 않는 칸 : 응답에 generated, required는 싣지 않음
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AiGenerated {

    /** 만드는 방식 */
    String value() default "uuid";
}
