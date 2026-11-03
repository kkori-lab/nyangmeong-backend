package com.nyangmeong.global.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [카카오 로컬 API 설정]
 * - app.kakao-local.client : fake(전주 고정 업체), kakao(실제 호출)
 * - app.kakao-local.rest-api-key : REST API 키, app.kakao-local.timeout-ms : 요청 제한 시간(0이면 기본값)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.kakao-local")
public class KakaoLocalProperties {

    /** 클라이언트 종류 */
    private String client = "fake";

    /** API 주소 */
    private String baseUrl = "https://dapi.kakao.com";

    /** REST API 키 */
    private String restApiKey = "";

    /** 요청 제한 시간(밀리초) */
    private long timeoutMs = 3000;
}
