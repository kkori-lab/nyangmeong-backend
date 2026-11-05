package com.nyangmeong.global.aitool;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * [AI 매니저 도구 1건]
 * - params 키 : 경로 변수·쿼리·요청 본문 칸 이름(camelCase)
 */
public record AiToolResponse(String name, String domain, AiToolKind kind, Call call, Map<String, Param> params,
        List<Rule> rules) {

    /** 호출 경로 */
    public record Call(String method, String path) {
    }

    /** 칸 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Param(@JsonProperty("in") String location, String type, String group, Boolean required,
            Integer minLength, Integer maxLength, Long min, Long max, Boolean positive, String pattern,
            Integer precision, Integer scale, List<Code> codes) {

        /**
         * 코드 값을 뺀 칸
         */
        public Param withoutCodes() {
            return new Param(location, type, group, required, minLength, maxLength, min, max, positive, pattern,
                    precision, scale, null);
        }

        /**
         * 코드 값을 붙인 칸
         */
        public Param withCodes(List<Code> codes) {
            return new Param(location, type, group, required, minLength, maxLength, min, max, positive, pattern,
                    precision, scale, codes);
        }
    }

    /** 공통코드 값 */
    public record Code(String cd, String nm) {
    }

    /** 규칙 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Rule(RequiredIf requiredIf, List<String> oneOf) {
    }

    /** 조건부 필수 */
    public record RequiredIf(String field, String when, String equals) {
    }
}
