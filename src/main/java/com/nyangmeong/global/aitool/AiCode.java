package com.nyangmeong.global.aitool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [AI 매니저 도구 칸의 공통코드 그룹]
 * - 요청 DTO 칸·@RequestParam에 붙임 -> 응답에 그룹의 사용 중 코드 목록
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AiCode {

    /** 상위 공통코드 */
    String value();
}
