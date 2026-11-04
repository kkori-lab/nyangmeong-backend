package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.SnsProfile;
import com.nyangmeong.global.external.SnsProvider;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.member.dto.SnsSignupClaims;

/**
 * [검사 - SNS 가입 확인 토큰]
 * - 발급한 값이 그대로 돌아온다 / 만료·변조·엉뚱한 문자열은 SNS_SIGNUP_TOKEN_INVALID
 */
class SnsSignupTokenManagerTest {

    private final FieldCryptoService crypto = cryptoService();
    private final SnsSignupTokenManager manager = new SnsSignupTokenManager(crypto);

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(properties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private SnsProfile profile() {
        return new SnsProfile(SnsProvider.NAVER, "naver-1", "new@example.com", "010-1234-5678", true);
    }

    private void assertInvalid(Throwable e) {
        assertThat(e).isInstanceOf(BaseException.class);
        assertThat(((BaseException) e).getErrorCode()).isEqualTo(ErrorCode.SNS_SIGNUP_TOKEN_INVALID);
    }

    @Test
    void 발급한_토큰을_풀면_넣은_값이_그대로_나온다() {
        ReflectionTestUtils.setField(manager, "validMinutes", 30L);

        SnsSignupClaims claims = manager.resolve(manager.issue(profile(), "01012345678"));

        assertThat(claims.snsOffrCd()).isEqualTo("USRC0007");
        assertThat(claims.offrAcntId()).isEqualTo("naver-1");
        assertThat(claims.email()).isEqualTo("new@example.com");
        assertThat(claims.mblTelno()).isEqualTo("01012345678");
        assertThat(claims.ageCnfrmd()).isTrue();
        assertThat(claims.expiresAt()).isGreaterThan(claims.issuedAt());
    }

    @Test
    void 번호가_없으면_null로_돌아온다() {
        ReflectionTestUtils.setField(manager, "validMinutes", 30L);

        assertThat(manager.resolve(manager.issue(profile(), null)).mblTelno()).isNull();
    }

    @Test
    void 유효시간이_지난_토큰은_거절한다() {
        ReflectionTestUtils.setField(manager, "validMinutes", -1L);
        String token = manager.issue(profile(), null);

        assertThatThrownBy(() -> manager.resolve(token)).satisfies(this::assertInvalid);
    }

    @Test
    void 변조하거나_엉뚱한_문자열은_거절한다() {
        ReflectionTestUtils.setField(manager, "validMinutes", 30L);
        String token = manager.issue(profile(), null);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> manager.resolve(tampered)).satisfies(this::assertInvalid);
        assertThatThrownBy(() -> manager.resolve("not-a-token")).satisfies(this::assertInvalid);
        assertThatThrownBy(() -> manager.resolve("")).satisfies(this::assertInvalid);
        assertThatThrownBy(() -> manager.resolve(crypto.encrypt("{\"a\":1}"))).satisfies(this::assertInvalid);
    }
}
