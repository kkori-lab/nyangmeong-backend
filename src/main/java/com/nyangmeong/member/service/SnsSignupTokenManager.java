package com.nyangmeong.member.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.SnsProfile;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.member.dto.SnsSignupClaims;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * [SNS 가입 확인 토큰]
 * - 신규 SNS 계정의 제공자·계정 ID·이메일·번호·연령 확인을 POST /members 로 이어 준다
 * - 저장소 없이 암호화(AES-GCM)로 변조를 막는다 : 개인정보가 들어 있어 서명만 하지 않는다
 * - 위조·만료·해석 불가는 모두 SNS_SIGNUP_TOKEN_INVALID
 */
@Component
@RequiredArgsConstructor
public class SnsSignupTokenManager {

    private final FieldCryptoService fieldCryptoService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** 토큰 유효시간(분) */
    @Value("${app.signup.sns-token-valid-minutes:30}")
    private long validMinutes;

    /**
     * 토큰 발급 : normalizedMblTelno 는 정규화한 번호, 없으면 null
     */
    public String issue(SnsProfile profile, String normalizedMblTelno) {
        Instant now = Instant.now();
        SnsSignupClaims claims = new SnsSignupClaims(profile.provider().getCode(), profile.accountId(),
                profile.email(), normalizedMblTelno, profile.ageCnfrmd(), now.getEpochSecond(),
                now.plus(Duration.ofMinutes(validMinutes)).getEpochSecond());
        return fieldCryptoService.encrypt(jsonMapper.writeValueAsString(claims));
    }

    /**
     * 토큰 확인
     */
    public SnsSignupClaims resolve(String token) {
        SnsSignupClaims claims;
        try {
            claims = jsonMapper.readValue(fieldCryptoService.decrypt(token), SnsSignupClaims.class);
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.SNS_SIGNUP_TOKEN_INVALID);
        }
        if (claims == null || claims.expiresAt() < Instant.now().getEpochSecond() || claims.snsOffrCd() == null
                || claims.offrAcntId() == null || claims.email() == null) {
            throw new CustomException(ErrorCode.SNS_SIGNUP_TOKEN_INVALID);
        }
        return claims;
    }
}
