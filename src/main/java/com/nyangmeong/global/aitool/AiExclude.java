package com.nyangmeong.global.aitool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [AI 매니저 도구 목록에서 뺄 칸]
 * - 요청 DTO 칸에 붙임 -> 그 칸만 싣지 않고 도구는 그대로 싣는다
 * - 도구 목록이 지원하지 않는 모양(중첩 객체·값 목록 등)이지만 AI가 몰라도 되는 칸에 쓴다
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiExclude {
}
