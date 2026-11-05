package com.nyangmeong.global.aitool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [AI 매니저 도구 표시]
 * - 컨트롤러 메서드에 붙이면 GET /internal/ai/tools 목록에 실림
 * - name : Python 설명(data/tool_prompts.yaml)과 짝짓는 키
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiTool {

    /** 도구 이름 */
    String name();

    /** 도메인 */
    String domain();

    /** 종류 */
    AiToolKind kind();
}
