package com.nyangmeong.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.auth.dto.SnsLoginResponse;
import com.nyangmeong.auth.dto.SnsLoginResult;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.SnsProfile;
import com.nyangmeong.global.external.SnsProvider;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.validation.PhoneNumberNormalizer;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.SnsSignupTokenManager;

import lombok.RequiredArgsConstructor;

/**
 * [SNS 로그인 처리] 제공자가 준 회원 정보로 로그인·가입 분기를 정한다
 * - 제공자 계정 → 연결된 회원이 있으면 로그인, 없으면 같은 이메일의 회원 여부로 중복·복구·신규 가입을 가른다
 * - 이메일이 같다는 이유로 기존 회원에 SNS 를 연결하거나 복구하지 않는다
 * - 연결된 정지 회원은 MEMBER_INACTIVE, 연결된 탈퇴 보관 회원은 RECOVERY_AVAILABLE
 * - 제공자 호출은 SnsLoginService 가 트랜잭션 밖에서 끝낸 뒤 이 클래스를 부른다
 */
@Service
@RequiredArgsConstructor
public class SnsLoginProcessor {

    private final MemberService memberService;
    private final SnsSignupTokenManager snsSignupTokenManager;
    private final FieldCryptoService fieldCryptoService;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 로그인·가입 분기 : 최종 로그인 기록과 관리자 로그인 기록을 한 트랜잭션으로
     */
    @Transactional
    public SnsLoginResult process(SnsProfile profile) {
        SnsProvider provider = profile.provider();
        Member linked = memberService.findBySnsAccount(provider.getCode(), profile.accountId());
        if (linked != null) {
            return loginLinked(linked);
        }
        if (profile.email() == null || profile.email().isBlank()) {
            throw new CustomException(ErrorCode.SNS_EMAIL_NOT_PROVIDED);
        }
        Member sameEmail = memberService.findForLogin(fieldCryptoService.emailHmac(profile.email()));
        if (sameEmail != null) {
            SnsLoginResponse response = MemberStatus.WITHDRAWN.is(sameEmail.getMbrSttsCd())
                    ? SnsLoginResponse.recoveryAvailable(profile.email())
                    : SnsLoginResponse.emailDuplicate(profile.email());
            return SnsLoginResult.notLoggedIn(response);
        }
        String mblTelno = normalizeOrNull(profile.mblTelno());
        return SnsLoginResult.notLoggedIn(SnsLoginResponse.signupRequired(provider.getCode(), profile.email(), mblTelno,
                profile.ageCnfrmd(), snsSignupTokenManager.issue(profile, mblTelno)));
    }

    /** 연결된 회원 : 정상이면 로그인, 최종 로그인 갱신 전 값으로 닉네임 재안내를 판단한다 */
    private SnsLoginResult loginLinked(Member member) {
        if (MemberStatus.WITHDRAWN.is(member.getMbrSttsCd())) {
            return SnsLoginResult.notLoggedIn(
                    SnsLoginResponse.recoveryAvailable(fieldCryptoService.decrypt(member.getAcntEmlEnc())));
        }
        if (!MemberStatus.NORMAL.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
        boolean ncnmReguide = "N".equals(member.getNcnmStngCmptnYn()) && member.getLastLgnDt() == null;
        memberService.recordLogin(member.getMbrSn());
        if (MemberAuthority.ADMIN.is(member.getMbrAuthrtCd())) {
            adminTaskLogService.record(member.getMbrSn(), AdminTaskType.LOGIN, "MBR", member.getMbrSn(), "관리자 로그인",
                    null, null);
        }
        return SnsLoginResult.login(member.getMbrSn(), SnsLoginResponse.login(ncnmReguide, member.getMbrNcnm()));
    }

    /** 제공자 번호 형식이 맞지 않으면 없는 것으로 보고 화면에서 직접 입력받는다 */
    private String normalizeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PhoneNumberNormalizer.normalize(raw);
        } catch (CustomException e) {
            return null;
        }
    }
}
