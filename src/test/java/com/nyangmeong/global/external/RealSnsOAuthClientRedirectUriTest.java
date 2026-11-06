package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * [SNS redirect-uri 결정]
 * - 설정값이 비면 프론트 주소의 콜백 경로, 있으면 설정값
 */
class RealSnsOAuthClientRedirectUriTest {

    @Test
    void 설정이_비면_프론트_주소로_콜백_경로를_만든다() {
        SnsOAuthProperties.Provider keys = new SnsOAuthProperties.Provider();

        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, keys, "http://localhost:5173"))
                .isEqualTo("http://localhost:5173/login/sns/kakao/callback");
        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.GOOGLE, keys, "https://front.example.com/"))
                .isEqualTo("https://front.example.com/login/sns/google/callback");
        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.NAVER, keys, "https://front.example.com"))
                .isEqualTo("https://front.example.com/login/sns/naver/callback");
    }

    @Test
    void 설정값이_있으면_그_값을_쓴다() {
        SnsOAuthProperties.Provider keys = new SnsOAuthProperties.Provider();
        keys.setRedirectUri("https://other.example.com/callback");

        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, keys, "http://localhost:5173"))
                .isEqualTo("https://other.example.com/callback");
    }
}
