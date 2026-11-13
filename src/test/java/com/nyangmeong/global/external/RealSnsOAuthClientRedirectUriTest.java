package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * [SNS redirect-uri 결정]
 * - 설정값이 비면 프론트 주소의 콜백 경로, 있으면 설정값
 * - 요청 Origin 이 허용 목록에 있으면 설정값보다 먼저 그 Origin 의 콜백 경로
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

    private static final List<String> ALLOWED = List.of("http://localhost:5173", "https://pawever.cloud/");

    @Test
    void 허용_목록의_Origin이면_설정값보다_먼저_그_Origin으로_콜백_경로를_만든다() {
        SnsOAuthProperties.Provider keys = new SnsOAuthProperties.Provider();
        keys.setRedirectUri("https://other.example.com/callback");

        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.GOOGLE, keys, "http://localhost:5173",
                "https://pawever.cloud", ALLOWED)).isEqualTo("https://pawever.cloud/login/sns/google/callback");
        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, keys, "https://pawever.cloud",
                "http://localhost:5173/", ALLOWED)).isEqualTo("http://localhost:5173/login/sns/kakao/callback");
    }

    @Test
    void 허용_목록_밖이거나_없는_Origin은_설정값_다음_프론트_주소를_쓴다() {
        SnsOAuthProperties.Provider empty = new SnsOAuthProperties.Provider();
        SnsOAuthProperties.Provider configured = new SnsOAuthProperties.Provider();
        configured.setRedirectUri("https://other.example.com/callback");

        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, configured, "http://localhost:5173",
                "https://evil.example.com", ALLOWED)).isEqualTo("https://other.example.com/callback");
        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, empty, "http://localhost:5173",
                "https://evil.example.com", ALLOWED)).isEqualTo("http://localhost:5173/login/sns/kakao/callback");
        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, empty, "http://localhost:5173", null, ALLOWED))
                .isEqualTo("http://localhost:5173/login/sns/kakao/callback");
        assertThat(RealSnsOAuthClient.redirectUri(SnsProvider.KAKAO, empty, "http://localhost:5173", " ", ALLOWED))
                .isEqualTo("http://localhost:5173/login/sns/kakao/callback");
    }
}
