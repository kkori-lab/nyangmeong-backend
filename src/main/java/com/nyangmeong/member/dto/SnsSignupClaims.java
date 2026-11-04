package com.nyangmeong.member.dto;

/**
 * [SNS 가입 확인 토큰에 담은 값]
 * - SnsSignupTokenManager 가 암호화해 signupTkn 으로 주고받는다
 * - mblTelno : 정규화한 번호, 제공자가 주지 않았거나 형식이 맞지 않으면 null
 * - issuedAt·expiresAt : 에포크 초
 */
public record SnsSignupClaims(String snsOffrCd, String offrAcntId, String email, String mblTelno, boolean ageCnfrmd,
        long issuedAt, long expiresAt) {
}
