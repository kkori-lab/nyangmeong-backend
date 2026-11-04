package com.nyangmeong.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * [SNS 로그인 응답]
 * - snsLoginRslt 에 따라 쓰는 필드가 다르다 (나머지는 null 이라 응답에서 빠진다)
 *   LOGIN : ncnmReguide·mbrNcnm, 로그인 쿠키가 함께 내려간다
 *   SIGNUP_REQUIRED : snsOffrCd·acntEml·mblTelno·ageCnfrmd·signupTkn
 *   EMAIL_DUPLICATE : acntEml
 *   RECOVERY_AVAILABLE : acntEml, 기존 이메일 복구 흐름으로 이어진다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnsLoginResponse(String snsLoginRslt, Boolean ncnmReguide, String mbrNcnm, String snsOffrCd,
        String acntEml, String mblTelno, Boolean ageCnfrmd, String signupTkn) {

    public static final String LOGIN = "LOGIN";
    public static final String SIGNUP_REQUIRED = "SIGNUP_REQUIRED";
    public static final String EMAIL_DUPLICATE = "EMAIL_DUPLICATE";
    public static final String RECOVERY_AVAILABLE = "RECOVERY_AVAILABLE";

    public static SnsLoginResponse login(boolean ncnmReguide, String mbrNcnm) {
        return new SnsLoginResponse(LOGIN, ncnmReguide, mbrNcnm, null, null, null, null, null);
    }

    public static SnsLoginResponse signupRequired(String snsOffrCd, String acntEml, String mblTelno,
            boolean ageCnfrmd, String signupTkn) {
        return new SnsLoginResponse(SIGNUP_REQUIRED, null, null, snsOffrCd, acntEml, mblTelno, ageCnfrmd, signupTkn);
    }

    public static SnsLoginResponse emailDuplicate(String acntEml) {
        return new SnsLoginResponse(EMAIL_DUPLICATE, null, null, null, acntEml, null, null, null);
    }

    public static SnsLoginResponse recoveryAvailable(String acntEml) {
        return new SnsLoginResponse(RECOVERY_AVAILABLE, null, null, null, acntEml, null, null, null);
    }
}
