package com.nyangmeong.global.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [SNS 로그인 설정]
 * - app.sns.client : real(실제 호출, 기본), fake(가짜 제공자, 로컬 확인용으로 직접 정했을 때만)
 * - app.sns.{naver,kakao,google}.client-id·client-secret·redirect-uri : 제공자 앱 키, redirect-uri 는 인가 요청과 같은 값, 비우면 app.front-base-url 의 /login/sns/{제공자}/callback
 * - app.sns.timeout-ms : 요청 제한 시간(0이면 기본값)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.sns")
public class SnsOAuthProperties {

    /** 클라이언트 종류 */
    private String client = "real";

    /** 요청 제한 시간(밀리초) */
    private long timeoutMs = 3000;

    private Provider naver = new Provider();
    private Provider kakao = new Provider();
    private Provider google = new Provider();

    public Provider of(SnsProvider provider) {
        return switch (provider) {
        case NAVER -> naver;
        case KAKAO -> kakao;
        case GOOGLE -> google;
        };
    }

    @Getter
    @Setter
    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
    }
}
