package com.nyangmeong.member.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.validation.PhoneNumberNormalizer;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberRecoverRequest;
import com.nyangmeong.member.dto.MemberRecoveryInfoRequest;
import com.nyangmeong.member.dto.MemberRecoveryResponse;
import com.nyangmeong.member.dto.TermsSummary;

import lombok.RequiredArgsConstructor;

/**
 * [탈퇴 회원 기존 정보 복구 서비스]
 * - 본인확인은 인증 확인 때 받은 복구 확인 권한 토큰이고, 안내 조회와 복구 실행에서 다시 검사한다
 * - 복구는 기존 회원번호를 정상으로 되돌리는 것이다, 새 계정·비밀번호·닉네임을 만들지 않고 자동 로그인도 하지 않는다
 */
@Service
@RequiredArgsConstructor
public class MemberRecoveryService {

    private final EmailCertificationService emailCertificationService;
    private final MemberService memberService;
    private final TermsService termsService;
    private final FieldCryptoService fieldCryptoService;
    private final EmailCertProperties emailCertProperties;

    /**
     * 복구 안내 조회 : 권한이 살아 있고 회원이 보관 기간 안일 때만
     */
    @Transactional(readOnly = true)
    public MemberRecoveryResponse getRecovery(MemberRecoveryInfoRequest req) {
        EmailCertification cert = emailCertificationService.checkRecoveryAuthority(req.emlCertSn(),
                req.rcvryAuthrtTkn());
        Member member = memberService.getRecoverableMember(cert.getCertTrgtEmlHmac());
        List<TermsSummary> missing = termsService.searchMissingEssential(member.getMbrSn());
        return new MemberRecoveryResponse(fieldCryptoService.decrypt(member.getAcntEmlEnc()), member.getMbrNcnm(),
                member.getWhdwlDt(), memberService.getRecoveryExpiryDt(member), maskPhone(member.getMblTelnoEnc()),
                missing);
    }

    /**
     * 복구 실행 : 권한 → 보관 기간 → 필수 약관 → 번호 → 권한 사용 → 상태·이력 순서, 하나라도 실패하면 모두 롤백
     * - 이미 쓴 권한이라도 그 회원이 지금 정상이고 권한 사용 직후면 바꾸는 것 없이 성공으로 답한다(중복 클릭·응답 유실)
     */
    @Transactional
    public void recover(MemberRecoverRequest req) {
        EmailCertification cert = emailCertificationService.findRecoveryCert(req.emlCertSn(), req.rcvryAuthrtTkn());
        if (cert.getCertUseCmptnDt() != null) {
            checkReplay(cert);
            return;
        }
        emailCertificationService.checkRecoveryUsable(cert);

        Member member = memberService.getRecoverableMember(cert.getCertTrgtEmlHmac());
        List<Long> agreedTrmsSns = termsService.checkRecoveryAgreements(
                termsService.searchMissingEssential(member.getMbrSn()), req.trmsAgres());
        String newMblTelno = null;
        if (member.getMblTelnoEnc() == null) {
            newMblTelno = PhoneNumberNormalizer.normalize(req.mblTelno());
            memberService.checkPhoneAvailable(newMblTelno);
        }

        emailCertificationService.useRecoveryAuthority(req.emlCertSn(), req.rcvryAuthrtTkn());
        memberService.recover(member, newMblTelno);
        termsService.createRecoveryAgreements(member.getMbrSn(), agreedTrmsSns);
    }

    /** 응답 유실·중복 클릭 재요청 : 권한을 쓴 지 얼마 안 됐고 그 이메일 회원이 정상이어야 한다 */
    private void checkReplay(EmailCertification cert) {
        Member member = memberService.findForLogin(cert.getCertTrgtEmlHmac());
        LocalDateTime replayUntil = cert.getCertUseCmptnDt().plusMinutes(emailCertProperties.getVerifiedKeepMinutes());
        if (member == null || !MemberStatus.NORMAL.is(member.getMbrSttsCd())
                || LocalDateTime.now().isAfter(replayUntil)) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE);
        }
    }

    /** 010-****-5678 : 보관 번호가 없으면 null (직접 입력) */
    private String maskPhone(String mblTelnoEnc) {
        if (mblTelnoEnc == null) {
            return null;
        }
        String mblTelno = fieldCryptoService.decrypt(mblTelnoEnc);
        return mblTelno.substring(0, 3) + "-****-" + mblTelno.substring(mblTelno.length() - 4);
    }
}
