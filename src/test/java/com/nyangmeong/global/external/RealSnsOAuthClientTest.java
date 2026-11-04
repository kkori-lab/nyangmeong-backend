package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 실제 SNS 제공자 응답 해석]
 * - 제공자가 인증하지 않은 이메일은 받지 않은 것으로 본다
 * - 실패 로그 요약에는 오류 코드·설명만 들어가고 토큰은 들어가지 않는다
 */
class RealSnsOAuthClientTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private JsonNode json(String body) {
        return jsonMapper.readTree(body);
    }

    @Test
    void 구글은_인증된_이메일만_받는다() {
        SnsProfile verified = RealSnsOAuthClient.toProfile(SnsProvider.GOOGLE,
                json("{\"sub\":\"g-1\",\"email\":\"a@gmail.com\",\"email_verified\":true}"));
        SnsProfile unverified = RealSnsOAuthClient.toProfile(SnsProvider.GOOGLE,
                json("{\"sub\":\"g-1\",\"email\":\"a@gmail.com\",\"email_verified\":false}"));
        SnsProfile missingFlag = RealSnsOAuthClient.toProfile(SnsProvider.GOOGLE,
                json("{\"sub\":\"g-1\",\"email\":\"a@gmail.com\"}"));

        assertThat(verified.email()).isEqualTo("a@gmail.com");
        assertThat(verified.accountId()).isEqualTo("g-1");
        assertThat(verified.mblTelno()).isNull();
        assertThat(verified.ageCnfrmd()).isFalse();
        assertThat(unverified.email()).isNull();
        assertThat(missingFlag.email()).isNull();
    }

    @Test
    void 카카오는_인증되고_유효한_이메일만_받는다() {
        SnsProfile verified = RealSnsOAuthClient.toProfile(SnsProvider.KAKAO, json("""
                {"id":12345,"kakao_account":{"email":"k@kakao.com","is_email_verified":true,"is_email_valid":true,
                 "phone_number":"+82 10-1234-5678"}}"""));
        SnsProfile notVerified = RealSnsOAuthClient.toProfile(SnsProvider.KAKAO, json("""
                {"id":12345,"kakao_account":{"email":"k@kakao.com","is_email_verified":false,"is_email_valid":true}}"""));
        SnsProfile notValid = RealSnsOAuthClient.toProfile(SnsProvider.KAKAO, json("""
                {"id":12345,"kakao_account":{"email":"k@kakao.com","is_email_verified":true,"is_email_valid":false}}"""));

        assertThat(verified.email()).isEqualTo("k@kakao.com");
        assertThat(verified.accountId()).isEqualTo("12345");
        assertThat(verified.mblTelno()).isEqualTo("+82 10-1234-5678");
        assertThat(notVerified.email()).isNull();
        assertThat(notValid.email()).isNull();
    }

    @Test
    void 네이버는_받은_이메일과_번호를_그대로_쓴다() {
        SnsProfile profile = RealSnsOAuthClient.toProfile(SnsProvider.NAVER, json("""
                {"resultcode":"00","response":{"id":"n-1","email":"n@naver.com","mobile":"010-1234-5678"}}"""));

        assertThat(profile.accountId()).isEqualTo("n-1");
        assertThat(profile.email()).isEqualTo("n@naver.com");
        assertThat(profile.mblTelno()).isEqualTo("010-1234-5678");
        assertThat(profile.ageCnfrmd()).isTrue();
    }

    @Test
    void 오류_요약은_오류_코드와_설명만_꺼낸다() {
        String naver = RealSnsOAuthClient.errorSummary(
                "{\"error\":\"invalid_request\",\"error_description\":\"no valid data in session\"}"
                        .getBytes(StandardCharsets.UTF_8));
        String kakao = RealSnsOAuthClient.errorSummary(
                "{\"error\":\"invalid_grant\",\"error_description\":\"authorization code not found\",\"error_code\":\"KOE320\"}"
                        .getBytes(StandardCharsets.UTF_8));

        assertThat(naver).isEqualTo("error=invalid_request error_description=no valid data in session");
        assertThat(kakao).contains("error=invalid_grant", "error_code=KOE320", "authorization code not found");
    }

    @Test
    void 오류_요약에는_토큰을_남기지_않는다() {
        String summary = RealSnsOAuthClient.errorSummary(
                "{\"access_token\":\"secret-token\",\"refresh_token\":\"secret-refresh\"}"
                        .getBytes(StandardCharsets.UTF_8));

        assertThat(summary).doesNotContain("secret").isEqualTo("(오류 필드 없음)");
        assertThat(RealSnsOAuthClient.errorSummary("<html>bad gateway</html>".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("(JSON 아님)");
        assertThat(RealSnsOAuthClient.errorSummary(new byte[0])).isEqualTo("(본문 없음)");
    }
}
