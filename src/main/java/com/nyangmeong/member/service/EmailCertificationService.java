package com.nyangmeong.member.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.CertificationMailEvent;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.domain.EmailCertPurpose;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.dto.EmailCertificationCreateRequest;
import com.nyangmeong.member.dto.EmailCertificationInvalidateRequest;
import com.nyangmeong.member.dto.EmailCertificationPasswordResetRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.EmailCertificationResult;
import com.nyangmeong.member.dto.EmailCertificationVerifyRequest;
import com.nyangmeong.member.dto.PasswordResetAccount;
import com.nyangmeong.member.mapper.EmailCertificationMapper;

import lombok.RequiredArgsConstructor;

/**
 * [이메일 인증 서비스]
 * - 가입용·비밀번호 찾기용·복구용 인증번호 발송·재전송·확인, 가입 때 인증 검사와 사용 처리
 * - 인증번호 원문은 저장하지 않고 이메일 HMAC 가 섞인 해시만 저장한다
 * - 비밀번호 찾기는 회원 조회 없이 발송하고 확인 때 계정을 가려 재설정 대상에게만 토큰을 준다
 * - 재설정 권한 토큰으로 비밀번호를 바꾸고 그 회원의 기존 로그인을 모두 끝낸다
 * - 복구는 탈퇴 보관 기간 안의 회원에게만 발송하고, 확인하면 복구 확인 권한 토큰을 준다(재설정 토큰과 같은 칸에 해시)
 */
@Service
@RequiredArgsConstructor
public class EmailCertificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MAIL_SUBJECT = "[냥멍케어] 이메일 인증번호 안내";
    private static final String PASSWORD_RESET_MAIL_SUBJECT = "[냥멍케어] 비밀번호 찾기 인증번호 안내";
    private static final String RECOVERY_MAIL_SUBJECT = "[냥멍케어] 재가입 복구 인증번호 안내";
    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;

    private final EmailCertificationMapper emailCertificationMapper;
    private final MemberService memberService;
    private final FieldCryptoService fieldCryptoService;
    private final EventPublisher eventPublisher;
    private final EmailCertProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionManager loginSessionManager;

    /**
     * 인증번호 발송·재전송 : 이전 번호는 무효, 간격·횟수 제한에 걸리면 재시도 가능 시각을 돌려준다
     */
    @Transactional
    public EmailCertificationResult send(EmailCertificationCreateRequest req) {
        EmailCertPurpose purpose = sendablePurpose(req.emlCertPrpsCd());
        String email = fieldCryptoService.normalizeEmail(req.certTrgtEml());
        String emailHmac = fieldCryptoService.hmac(email);
        /** 비밀번호 찾기는 가입 여부가 드러나지 않게 회원을 조회하지 않는다 */
        if (purpose == EmailCertPurpose.SIGNUP) {
            memberService.checkEmailAvailable(emailHmac);
        } else if (purpose == EmailCertPurpose.RECOVERY) {
            memberService.getRecoverableMember(emailHmac);
        }

        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> recent = emailCertificationMapper.selectRegDtList(emailHmac, purpose.getCode(),
                now.minusMinutes(properties.getResendWindowMinutes()));
        LocalDateTime resendAvailableDt = resendAvailableAt(recent, now);
        if (resendAvailableDt != null) {
            return EmailCertificationResult.fail(ErrorCode.EMAIL_CERT_RESEND_NOT_YET,
                    EmailCertificationResponse.resendLimited(resendAvailableDt));
        }

        emailCertificationMapper.updateInvalidatePrevious(emailHmac, purpose.getCode(), now);
        String certNo = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailCertification cert = new EmailCertification();
        cert.setEmlCertPrpsCd(purpose.getCode());
        cert.setCertTrgtEmlEnc(fieldCryptoService.encrypt(email));
        cert.setCertTrgtEmlHmac(emailHmac);
        cert.setCertNoHash(certNoHash(emailHmac, certNo));
        cert.setCertExpryDt(now.plusSeconds(properties.getCodeValiditySeconds()));
        cert.setRegDt(now);
        emailCertificationMapper.insert(cert);

        long minutes = Math.max(1, (properties.getCodeValiditySeconds() + 59) / 60);
        eventPublisher.publish(new CertificationMailEvent(email, mailSubject(purpose),
                "인증번호는 " + certNo + " 입니다. " + minutes + "분 안에 입력해 주세요."));
        return EmailCertificationResult.success(EmailCertificationResponse.sent(cert));
    }

    /**
     * 인증번호 확인 : 틀린 횟수는 오류로 끝나도 남아야 해서 CustomException 에도 롤백하지 않는다
     */
    @Transactional(noRollbackFor = CustomException.class)
    public EmailCertificationResponse verify(Long emlCertSn, EmailCertificationVerifyRequest req) {
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        if (cert == null || !isVerifiablePurpose(cert.getEmlCertPrpsCd()) || cert.getCertInvldDt() != null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_FOUND);
        }
        if (cert.getCertUseCmptnDt() != null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_ALREADY_USED);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isCompleted = cert.getCertCmptnDt() != null;
        if (isCompleted ? isVerifiedExpired(cert, now) : now.isAfter(cert.getCertExpryDt())) {
            throw new CustomException(ErrorCode.EMAIL_CERT_EXPIRED);
        }
        checkCertNo(cert, req.certNo(), now);
        boolean isPasswordReset = EmailCertPurpose.PASSWORD_RESET.is(cert.getEmlCertPrpsCd());
        boolean isRecovery = EmailCertPurpose.RECOVERY.is(cert.getEmlCertPrpsCd());
        if (isRecovery) {
            memberService.getRecoverableMember(cert.getCertTrgtEmlHmac());
        } else if (!isPasswordReset) {
            memberService.checkEmailAvailable(cert.getCertTrgtEmlHmac());
        }

        if (!isCompleted) {
            emailCertificationMapper.updateCompleted(emlCertSn, now);
        }
        LocalDateTime completedDt = isCompleted ? cert.getCertCmptnDt() : now;
        if (isRecovery) {
            return verifiedForRecovery(cert, completedDt);
        }
        return isPasswordReset ? verifiedForPasswordReset(cert, completedDt)
                : EmailCertificationResponse.verified(cert, completedDt);
    }

    /**
     * 비밀번호 찾기·복구 인증 무효화 : 일련번호는 순번이라 인증한 이메일이 맞아야 한다, 이미 무효·사용 완료면 그대로 둔다
     */
    @Transactional
    public void invalidate(Long emlCertSn, EmailCertificationInvalidateRequest req) {
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        String emailHmac = fieldCryptoService.hmac(fieldCryptoService.normalizeEmail(req.certTrgtEml()));
        if (cert == null || !isInvalidatablePurpose(cert.getEmlCertPrpsCd())
                || !emailHmac.equals(cert.getCertTrgtEmlHmac())) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_FOUND);
        }
        if (cert.getCertInvldDt() == null && cert.getCertUseCmptnDt() == null) {
            emailCertificationMapper.updateInvalidate(emlCertSn, LocalDateTime.now());
        }
    }

    /**
     * 비밀번호 재설정 : 입력 검증 → 권한 확인 → 저장 순서, 권한 문제는 이유를 가리지 않고 한 오류로 답한다
     * - 사용 처리가 먼저 1행을 잡아야 해서 같은 토큰을 동시에 써도 한 요청만 바뀌고, 하나라도 실패하면 모두 롤백한다
     */
    @Transactional
    public void resetPassword(Long emlCertSn, EmailCertificationPasswordResetRequest req) {
        if (!req.pswd().equals(req.pswdCnfrm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        if (cert == null || !EmailCertPurpose.PASSWORD_RESET.is(cert.getEmlCertPrpsCd())) {
            throw new CustomException(ErrorCode.PASSWORD_RESET_NOT_AVAILABLE);
        }
        PasswordResetAccount account = memberService.resolvePasswordResetAccount(cert.getCertTrgtEmlHmac());
        if (!account.resettable()) {
            throw new CustomException(ErrorCode.PASSWORD_RESET_NOT_AVAILABLE);
        }
        int claimed = emailCertificationMapper.updatePasswordResetUsed(emlCertSn,
                EmailCertPurpose.PASSWORD_RESET.getCode(), fieldCryptoService.hmac("pswdinit:" + req.pswdInitTkn()),
                LocalDateTime.now());
        if (claimed != 1) {
            throw new CustomException(ErrorCode.PASSWORD_RESET_NOT_AVAILABLE);
        }
        memberService.changePassword(account.mbrSn(), passwordEncoder.encode(req.pswd()));
        loginSessionManager.revokeAll(account.mbrSn());
    }

    /**
     * 복구 확인 권한 인증 찾기 : 복구 목적·인증 완료·무효화 안 됨·토큰 일치, 쓴 것인지와 만료는 보지 않는다
     * - 권한 문제는 이유를 가리지 않고 한 오류로 답한다
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public EmailCertification findRecoveryCert(Long emlCertSn, String rcvryAuthrtTkn) {
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        if (cert == null || !EmailCertPurpose.RECOVERY.is(cert.getEmlCertPrpsCd()) || cert.getCertCmptnDt() == null
                || cert.getCertInvldDt() != null || cert.getPswdInitTknHash() == null
                || !MessageDigest.isEqual(cert.getPswdInitTknHash().getBytes(StandardCharsets.UTF_8),
                        recoveryTokenHash(rcvryAuthrtTkn).getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE);
        }
        return cert;
    }

    /**
     * 복구 확인 권한이 아직 쓸 수 있는지 : 쓰지 않았고 권한 만료 전
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public void checkRecoveryUsable(EmailCertification cert) {
        if (cert.getCertUseCmptnDt() != null || cert.getInitAuthrtExpryDt() == null
                || !cert.getInitAuthrtExpryDt().isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE);
        }
    }

    /**
     * 복구 확인 권한 확인 : 찾기 + 쓸 수 있는지
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public EmailCertification checkRecoveryAuthority(Long emlCertSn, String rcvryAuthrtTkn) {
        EmailCertification cert = findRecoveryCert(emlCertSn, rcvryAuthrtTkn);
        checkRecoveryUsable(cert);
        return cert;
    }

    /**
     * 복구 확인 권한 사용 처리 : 한 번만, 먼저 쓴 요청이 있으면 1행이 아니라서 거절
     */
    @Transactional
    public void useRecoveryAuthority(Long emlCertSn, String rcvryAuthrtTkn) {
        int claimed = emailCertificationMapper.updatePasswordResetUsed(emlCertSn, EmailCertPurpose.RECOVERY.getCode(),
                recoveryTokenHash(rcvryAuthrtTkn), LocalDateTime.now());
        if (claimed != 1) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE);
        }
    }

    /**
     * 가입 때 인증 검사 : 별도 트랜잭션이라 번호가 틀려 가입이 롤백돼도 실패 횟수는 남는다
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CustomException.class)
    public EmailCertification checkForSignup(Long emlCertSn, String certNo) {
        EmailCertification cert = emailCertificationMapper.selectBySn(emlCertSn);
        if (cert == null || !EmailCertPurpose.SIGNUP.is(cert.getEmlCertPrpsCd()) || cert.getCertInvldDt() != null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        checkCertNo(cert, certNo, now);
        if (cert.getCertCmptnDt() == null) {
            throw new CustomException(ErrorCode.EMAIL_CERT_NOT_VERIFIED);
        }
        /** 이미 쓴 인증은 호출한 쪽이 같은 가입의 재요청인지 가린다 */
        if (cert.getCertUseCmptnDt() == null && isVerifiedExpired(cert, now)) {
            throw new CustomException(ErrorCode.EMAIL_CERT_EXPIRED);
        }
        return cert;
    }

    /**
     * 인증 사용 처리 : 한 번만, 먼저 쓴 요청이 있으면 처리 중 오류
     */
    @Transactional
    public void use(Long emlCertSn) {
        if (emailCertificationMapper.updateUsed(emlCertSn, LocalDateTime.now()) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
    }

    /**
     * 비밀번호 찾기 인증 확인 뒤 계정 분기 : 재설정 대상에게만 토큰을 준다
     * - 같은 번호로 다시 확인하면 새 토큰으로 바꾸고 이전 토큰은 쓸 수 없다, 권한 만료는 인증 완료 시각 기준 그대로
     */
    private EmailCertificationResponse verifiedForPasswordReset(EmailCertification cert, LocalDateTime completedDt) {
        PasswordResetAccount account = memberService.resolvePasswordResetAccount(cert.getCertTrgtEmlHmac());
        if (!account.resettable()) {
            emailCertificationMapper.updatePasswordResetToken(cert.getEmlCertSn(), null, null);
            return EmailCertificationResponse.passwordReset(cert, completedDt, account, null, null);
        }
        byte[] bytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime authrtExpryDt = completedDt.plusMinutes(properties.getVerifiedKeepMinutes());
        emailCertificationMapper.updatePasswordResetToken(cert.getEmlCertSn(),
                fieldCryptoService.hmac("pswdinit:" + token), authrtExpryDt);
        return EmailCertificationResponse.passwordReset(cert, completedDt, account, token, authrtExpryDt);
    }

    /**
     * 복구 인증 확인 뒤 토큰 발급 : 같은 번호로 다시 확인하면 새 토큰이고 이전 토큰은 쓸 수 없다, 권한 만료는 인증 완료 시각 기준 그대로
     */
    private EmailCertificationResponse verifiedForRecovery(EmailCertification cert, LocalDateTime completedDt) {
        byte[] bytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime authrtExpryDt = completedDt.plusMinutes(properties.getVerifiedKeepMinutes());
        emailCertificationMapper.updatePasswordResetToken(cert.getEmlCertSn(), recoveryTokenHash(token),
                authrtExpryDt);
        return EmailCertificationResponse.recovery(cert, completedDt, token, authrtExpryDt);
    }

    private String recoveryTokenHash(String token) {
        return fieldCryptoService.hmac("rcvry:" + token);
    }

    private String mailSubject(EmailCertPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> MAIL_SUBJECT;
            case PASSWORD_RESET -> PASSWORD_RESET_MAIL_SUBJECT;
            case RECOVERY -> RECOVERY_MAIL_SUBJECT;
        };
    }

    /** 발송 목적 : 가입·비밀번호 찾기·복구 */
    private EmailCertPurpose sendablePurpose(String code) {
        if (EmailCertPurpose.SIGNUP.is(code)) {
            return EmailCertPurpose.SIGNUP;
        }
        if (EmailCertPurpose.PASSWORD_RESET.is(code)) {
            return EmailCertPurpose.PASSWORD_RESET;
        }
        if (EmailCertPurpose.RECOVERY.is(code)) {
            return EmailCertPurpose.RECOVERY;
        }
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private boolean isVerifiablePurpose(String code) {
        return EmailCertPurpose.SIGNUP.is(code) || EmailCertPurpose.PASSWORD_RESET.is(code)
                || EmailCertPurpose.RECOVERY.is(code);
    }

    /** 가입 인증은 가입 요청이 쓰므로 무효화 대상이 아니다 */
    private boolean isInvalidatablePurpose(String code) {
        return EmailCertPurpose.PASSWORD_RESET.is(code) || EmailCertPurpose.RECOVERY.is(code);
    }

    /** 틀리면 횟수를 올리고, 제한에 닿으면 그 인증을 무효화 */
    private void checkCertNo(EmailCertification cert, String certNo, LocalDateTime now) {
        if (certNoHash(cert.getCertTrgtEmlHmac(), certNo).equals(cert.getCertNoHash())) {
            return;
        }
        emailCertificationMapper.updateFailCount(cert.getEmlCertSn());
        if (cert.getCertFailNmtm() + 1 >= properties.getFailLimit()) {
            emailCertificationMapper.updateInvalidate(cert.getEmlCertSn(), now);
            throw new CustomException(ErrorCode.EMAIL_CERT_FAIL_LIMIT_EXCEEDED);
        }
        throw new CustomException(ErrorCode.EMAIL_CERT_MISMATCH);
    }

    /** 인증 완료 결과를 가입에 쓸 수 있는 시간이 지났는지 */
    private boolean isVerifiedExpired(EmailCertification cert, LocalDateTime now) {
        return now.isAfter(cert.getCertCmptnDt().plusMinutes(properties.getVerifiedKeepMinutes()));
    }

    /** 다시 보낼 수 있는 시각, 바로 보낼 수 있으면 null */
    private LocalDateTime resendAvailableAt(List<LocalDateTime> recent, LocalDateTime now) {
        LocalDateTime availableAt = null;
        if (!recent.isEmpty()) {
            LocalDateTime byInterval = recent.get(recent.size() - 1).plusSeconds(properties.getResendIntervalSeconds());
            if (now.isBefore(byInterval)) {
                availableAt = byInterval;
            }
        }
        if (recent.size() >= properties.getResendMaxCount()) {
            LocalDateTime byCount = recent.get(recent.size() - properties.getResendMaxCount())
                    .plusMinutes(properties.getResendWindowMinutes());
            if (availableAt == null || byCount.isAfter(availableAt)) {
                availableAt = byCount;
            }
        }
        return availableAt;
    }

    private String certNoHash(String emailHmac, String certNo) {
        return fieldCryptoService.hmac("emlcert:" + emailHmac + ":" + certNo);
    }
}
