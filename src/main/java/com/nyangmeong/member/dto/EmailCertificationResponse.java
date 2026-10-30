package com.nyangmeong.member.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nyangmeong.member.domain.EmailCertification;

/**
 * [이메일 인증 응답]
 * - 발송 : 만료시각 / 재전송 제한 : 재시도 가능 시각만 / 확인 : 인증 완료 시각과 결과
 * - 가입 목적 결과는 SIGNUP_READY, 비밀번호 찾기 목적은 계정 분기 결과와 재설정 토큰(재설정 대상일 때만)
 * - 복구 목적은 RECOVERY_READY 와 복구 확인 권한 토큰(rcvryAuthrtTkn), 권한 만료는 initAuthrtExpryDt
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailCertificationResponse(Long emlCertSn, String emlCertPrpsCd, LocalDateTime certExpryDt,
        LocalDateTime resendAvailableDt, LocalDateTime certCmptnDt, String certRslt, List<String> snsOffrCds,
        String pswdInitTkn, LocalDateTime initAuthrtExpryDt, String rcvryAuthrtTkn) {

    public static final String SIGNUP_READY = "SIGNUP_READY";
    public static final String PASSWORD_RESET_READY = "PASSWORD_RESET_READY";
    public static final String SNS_ONLY = "SNS_ONLY";
    public static final String MEMBER_NOT_FOUND = "MEMBER_NOT_FOUND";
    public static final String MEMBER_WITHDRAWN = "MEMBER_WITHDRAWN";
    public static final String RECOVERY_READY = "RECOVERY_READY";

    public EmailCertificationResponse(Long emlCertSn, String emlCertPrpsCd, LocalDateTime certExpryDt,
            LocalDateTime resendAvailableDt, LocalDateTime certCmptnDt, String certRslt, List<String> snsOffrCds,
            String pswdInitTkn, LocalDateTime initAuthrtExpryDt) {
        this(emlCertSn, emlCertPrpsCd, certExpryDt, resendAvailableDt, certCmptnDt, certRslt, snsOffrCds, pswdInitTkn,
                initAuthrtExpryDt, null);
    }

    public EmailCertificationResponse(Long emlCertSn, String emlCertPrpsCd, LocalDateTime certExpryDt,
            LocalDateTime resendAvailableDt, LocalDateTime certCmptnDt, String certRslt) {
        this(emlCertSn, emlCertPrpsCd, certExpryDt, resendAvailableDt, certCmptnDt, certRslt, null, null, null, null);
    }

    public static EmailCertificationResponse sent(EmailCertification cert) {
        return new EmailCertificationResponse(cert.getEmlCertSn(), cert.getEmlCertPrpsCd(), cert.getCertExpryDt(),
                null, null, null);
    }

    public static EmailCertificationResponse resendLimited(LocalDateTime resendAvailableDt) {
        return new EmailCertificationResponse(null, null, null, resendAvailableDt, null, null);
    }

    public static EmailCertificationResponse verified(EmailCertification cert, LocalDateTime certCmptnDt) {
        return new EmailCertificationResponse(cert.getEmlCertSn(), cert.getEmlCertPrpsCd(), cert.getCertExpryDt(),
                null, certCmptnDt, SIGNUP_READY);
    }

    public static EmailCertificationResponse passwordReset(EmailCertification cert, LocalDateTime certCmptnDt,
            PasswordResetAccount account, String pswdInitTkn, LocalDateTime initAuthrtExpryDt) {
        return new EmailCertificationResponse(cert.getEmlCertSn(), cert.getEmlCertPrpsCd(), cert.getCertExpryDt(),
                null, certCmptnDt, account.certRslt(), account.snsOffrCds(), pswdInitTkn, initAuthrtExpryDt);
    }

    public static EmailCertificationResponse recovery(EmailCertification cert, LocalDateTime certCmptnDt,
            String rcvryAuthrtTkn, LocalDateTime initAuthrtExpryDt) {
        return new EmailCertificationResponse(cert.getEmlCertSn(), cert.getEmlCertPrpsCd(), cert.getCertExpryDt(),
                null, certCmptnDt, RECOVERY_READY, null, null, initAuthrtExpryDt, rcvryAuthrtTkn);
    }
}
