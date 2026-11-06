package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * [검사 - SNS 제공자 오류 로그 가리기]
 * - 제공자가 오류 설명에 인가 코드·토큰을 실어 보내도 로그 요약에는 값이 남지 않는다
 */
class RealSnsOAuthClientLogMaskTest {

    private static String summary(String body) {
        return RealSnsOAuthClient.errorSummary(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void 카카오_오류_설명에_실린_인가_코드는_가린다() {
        String kakao = summary("{\"error\":\"invalid_grant\",\"error_description\":"
                + "\"authorization code not found for code=9QTlU3mz8CaW-_r2\",\"error_code\":\"KOE320\"}");

        assertThat(kakao).doesNotContain("9QTlU3mz8CaW-_r2")
                .contains("error=invalid_grant", "error_code=KOE320", "authorization code not found for code=***");
    }

    @Test
    void 오류_설명의_토큰_값도_가린다() {
        String result = summary("{\"error\":\"invalid_token\",\"error_description\":\"bad token: abc.def.ghi\"}");

        assertThat(result).doesNotContain("abc.def.ghi").contains("token: ***");
    }

    @Test
    void 코드나_토큰이_없는_설명은_그대로_둔다() {
        assertThat(summary("{\"error\":\"invalid_request\",\"error_description\":\"no valid data in session\"}"))
                .isEqualTo("error=invalid_request error_description=no valid data in session");
    }
}
