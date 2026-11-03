package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.member.domain.EmailCertPurpose;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberRecoverRequest;
import com.nyangmeong.member.dto.MemberRecoveryInfoRequest;
import com.nyangmeong.member.dto.MemberRecoveryResponse;
import com.nyangmeong.member.dto.TermsAgreeRequest;
import com.nyangmeong.member.dto.TermsSummary;

/**
 * [검사 - 탈퇴 회원 기존 정보 복구 서비스]
 * - 안내 조회: 권한 확인 뒤 이메일·닉네임·보관 만료·가린 번호·빠진 약관
 * - 복구 실행: 권한 → 보관 기간 → 약관 → 번호 → 권한 사용 → 상태·이력, 재요청은 바꾸는 것 없이 성공
 */
class MemberRecoveryServiceTest {

    private static final String EMAIL = "newbie@example.com";
    private static final String PHONE = "01012345678";
    private static final String TOKEN = "recovery-token";

    private final EmailCertificationService emailCertificationService = mock(EmailCertificationService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final TermsService termsService = mock(TermsService.class);
    private final FieldCryptoService crypto = cryptoService();
    private final EmailCertProperties properties = new EmailCertProperties();
    private final MemberRecoveryService service = new MemberRecoveryService(emailCertificationService, memberService,
            termsService, crypto, properties);

    private final String emailHmac = crypto.hmac(EMAIL);
    private final LocalDateTime whdwlDt = LocalDateTime.now().minusDays(3);

    private static FieldCryptoService cryptoService() {
        CryptoProperties cryptoProperties = new CryptoProperties();
        cryptoProperties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        cryptoProperties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(cryptoProperties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private Member withdrawnMember(boolean hasPhone) {
        Member member = new Member();
        member.setMbrSn(7L);
        member.setMbrSttsCd(MemberStatus.WITHDRAWN.getCode());
        member.setAcntEmlEnc(crypto.encrypt(EMAIL));
        member.setMblTelnoEnc(hasPhone ? crypto.encrypt(PHONE) : null);
        member.setMbrNcnm("초코맘");
        member.setWhdwlDt(whdwlDt);
        return member;
    }

    private EmailCertification cert(LocalDateTime usedDt) {
        EmailCertification cert = new EmailCertification();
        cert.setEmlCertSn(3L);
        cert.setEmlCertPrpsCd(EmailCertPurpose.RECOVERY.getCode());
        cert.setCertTrgtEmlHmac(emailHmac);
        cert.setCertUseCmptnDt(usedDt);
        return cert;
    }

    private TermsSummary terms(long trmsSn) {
        TermsSummary summary = new TermsSummary();
        summary.setTrmsSn(trmsSn);
        summary.setEsntlAgreYn(true);
        return summary;
    }

    private MemberRecoverRequest recoverRequest(String mblTelno) {
        return new MemberRecoverRequest(3L, TOKEN, List.of(new TermsAgreeRequest(2L, true)), mblTelno);
    }

    /** 복구 권한이 살아 있고 보관 기간 안의 탈퇴 회원 7번이 준비된 상태 */
    private Member readyRecover(boolean hasPhone) {
        Member member = withdrawnMember(hasPhone);
        EmailCertification cert = cert(null);
        when(emailCertificationService.findRecoveryCert(3L, TOKEN)).thenReturn(cert);
        when(memberService.getRecoverableMember(emailHmac)).thenReturn(member);
        when(termsService.searchMissingEssential(7L)).thenReturn(List.of(terms(2L)));
        when(termsService.checkRecoveryAgreements(any(), any())).thenReturn(List.of(2L));
        return member;
    }

    @Test
    void 안내는_이메일_닉네임_보관_만료_가린_번호_빠진_약관을_준다() {
        Member member = withdrawnMember(true);
        when(emailCertificationService.checkRecoveryAuthority(3L, TOKEN)).thenReturn(cert(null));
        when(memberService.getRecoverableMember(emailHmac)).thenReturn(member);
        when(memberService.getRecoveryExpiryDt(member)).thenReturn(whdwlDt.plusDays(30));
        when(termsService.searchMissingEssential(7L)).thenReturn(List.of(terms(2L)));

        MemberRecoveryResponse response = service.getRecovery(new MemberRecoveryInfoRequest(3L, TOKEN));

        assertThat(response.acntEml()).isEqualTo(EMAIL);
        assertThat(response.mbrNcnm()).isEqualTo("초코맘");
        assertThat(response.whdwlDt()).isEqualTo(whdwlDt);
        assertThat(response.rcvryExpryDt()).isEqualTo(whdwlDt.plusDays(30));
        assertThat(response.maskedMblTelno()).isEqualTo("010-****-5678");
        assertThat(response.missingTrms()).extracting(TermsSummary::getTrmsSn).containsExactly(2L);
    }

    @Test
    void 보관_번호가_없으면_안내의_가린_번호는_없다() {
        Member member = withdrawnMember(false);
        when(emailCertificationService.checkRecoveryAuthority(3L, TOKEN)).thenReturn(cert(null));
        when(memberService.getRecoverableMember(emailHmac)).thenReturn(member);
        when(memberService.getRecoveryExpiryDt(member)).thenReturn(whdwlDt.plusDays(30));
        when(termsService.searchMissingEssential(7L)).thenReturn(List.of());

        MemberRecoveryResponse response = service.getRecovery(new MemberRecoveryInfoRequest(3L, TOKEN));

        assertThat(response.maskedMblTelno()).isNull();
        assertThat(response.missingTrms()).isEmpty();
    }

    @Test
    void 권한이_없으면_안내하지_않고_회원도_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE)).when(emailCertificationService)
                .checkRecoveryAuthority(3L, TOKEN);

        assertThatThrownBy(() -> service.getRecovery(new MemberRecoveryInfoRequest(3L, TOKEN)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        verify(memberService, never()).getRecoverableMember(anyString());
    }

    @Test
    void 복구하면_권한을_쓰고_상태와_이력을_바꾸고_빠진_약관_동의를_저장한다() {
        Member member = readyRecover(true);

        service.recover(recoverRequest(null));

        InOrder order = inOrder(emailCertificationService, memberService, termsService);
        order.verify(emailCertificationService).useRecoveryAuthority(3L, TOKEN);
        order.verify(memberService).recover(member, null);
        order.verify(termsService).createRecoveryAgreements(7L, List.of(2L));
        verify(memberService, never()).checkPhoneAvailable(anyString());
    }

    @Test
    void 보관_번호가_없으면_정규화해_중복을_보고_그_번호로_복구한다() {
        Member member = readyRecover(false);

        service.recover(recoverRequest("010-1234-5678"));

        verify(memberService).checkPhoneAvailable(PHONE);
        verify(memberService).recover(member, PHONE);
    }

    @Test
    void 보관_번호가_있으면_보낸_번호는_무시한다() {
        Member member = readyRecover(true);

        service.recover(recoverRequest("010-9999-9999"));

        verify(memberService, never()).checkPhoneAvailable(anyString());
        verify(memberService).recover(member, null);
    }

    @Test
    void 보관_번호가_없는데_번호를_안_주거나_형식이_틀리면_거절하고_아무것도_바꾸지_않는다() {
        readyRecover(false);

        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_PHONE_NUMBER));
        assertThatThrownBy(() -> service.recover(recoverRequest("12345")))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_PHONE_NUMBER));

        verify(emailCertificationService, never()).useRecoveryAuthority(anyLong(), anyString());
        verify(memberService, never()).recover(any(), any());
    }

    @Test
    void 번호가_이미_쓰이면_거절하고_권한도_쓰지_않는다() {
        readyRecover(false);
        doThrow(new CustomException(ErrorCode.PHONE_ALREADY_USED)).when(memberService).checkPhoneAvailable(PHONE);

        assertThatThrownBy(() -> service.recover(recoverRequest(PHONE)))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));

        verify(emailCertificationService, never()).useRecoveryAuthority(anyLong(), anyString());
        verify(memberService, never()).recover(any(), any());
    }

    @Test
    void 빠진_필수_약관에_동의하지_않으면_거절하고_아무것도_바꾸지_않는다() {
        readyRecover(true);
        doThrow(new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED)).when(termsService)
                .checkRecoveryAgreements(any(), any());

        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));

        verify(emailCertificationService, never()).useRecoveryAuthority(anyLong(), anyString());
        verify(memberService, never()).recover(any(), any());
        verify(termsService, never()).createRecoveryAgreements(anyLong(), any());
    }

    @Test
    void 권한이_없거나_쓸_수_없으면_회원도_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE)).when(emailCertificationService)
                .findRecoveryCert(3L, TOKEN);
        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        EmailCertification expired = cert(null);
        when(emailCertificationService.findRecoveryCert(4L, TOKEN)).thenReturn(expired);
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE)).when(emailCertificationService)
                .checkRecoveryUsable(expired);
        assertThatThrownBy(() -> service.recover(new MemberRecoverRequest(4L, TOKEN, null, null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        verify(memberService, never()).getRecoverableMember(anyString());
        verify(memberService, never()).recover(any(), any());
    }

    @Test
    void 보관_기간이_지났으면_복구하지_않는다() {
        readyRecover(true);
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_EXPIRED)).when(memberService)
                .getRecoverableMember(emailHmac);

        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_EXPIRED));

        verify(emailCertificationService, never()).useRecoveryAuthority(anyLong(), anyString());
        verify(memberService, never()).recover(any(), any());
    }

    @Test
    void 상태_변경이_기간_경과로_실패하면_약관_동의도_저장하지_않는다() {
        Member member = readyRecover(true);
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_EXPIRED)).when(memberService).recover(member, null);

        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_EXPIRED));

        verify(termsService, never()).createRecoveryAgreements(anyLong(), any());
    }

    @Test
    void 권한_사용이_동시에_먼저_처리되면_거절한다() {
        readyRecover(true);
        doThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE)).when(emailCertificationService)
                .useRecoveryAuthority(3L, TOKEN);

        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        verify(memberService, never()).recover(any(), any());
    }

    @Test
    void 같은_권한으로_다시_보내도_회원이_정상이면_바꾸는_것_없이_성공한다() {
        EmailCertification used = cert(LocalDateTime.now().minusMinutes(1));
        Member recovered = withdrawnMember(true);
        recovered.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        when(emailCertificationService.findRecoveryCert(3L, TOKEN)).thenReturn(used);
        when(memberService.findForLogin(emailHmac)).thenReturn(recovered);

        service.recover(recoverRequest(null));

        verify(emailCertificationService, never()).checkRecoveryUsable(any());
        verify(emailCertificationService, never()).useRecoveryAuthority(anyLong(), anyString());
        verify(memberService, never()).recover(any(), any());
        verify(termsService, never()).createRecoveryAgreements(anyLong(), any());
    }

    @Test
    void 쓴_권한의_재요청도_회원이_정상이_아니거나_시간이_지났으면_거절한다() {
        EmailCertification used = cert(LocalDateTime.now().minusMinutes(1));
        when(emailCertificationService.findRecoveryCert(3L, TOKEN)).thenReturn(used);
        when(memberService.findForLogin(emailHmac)).thenReturn(withdrawnMember(true));
        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        EmailCertification longAgo = cert(LocalDateTime.now().minusMinutes(31));
        Member normal = withdrawnMember(true);
        normal.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        when(emailCertificationService.findRecoveryCert(4L, TOKEN)).thenReturn(longAgo);
        when(memberService.findForLogin(emailHmac)).thenReturn(normal);
        assertThatThrownBy(() -> service.recover(new MemberRecoverRequest(4L, TOKEN, null, null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        when(memberService.findForLogin(emailHmac)).thenReturn(null);
        assertThatThrownBy(() -> service.recover(recoverRequest(null)))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));
    }
}
