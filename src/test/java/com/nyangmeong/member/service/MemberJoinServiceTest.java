package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.SnsProfile;
import com.nyangmeong.global.external.SnsProvider;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.member.domain.EmailCertification;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberCreateRequest;
import com.nyangmeong.member.dto.MemberJoinResult;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.SnsSignupClaims;
import com.nyangmeong.member.dto.TermsAgreeRequest;

/**
 * [검사 - 회원가입 서비스]
 * - 검사를 모두 통과한 뒤에만 인증을 쓰고 회원·약관을 저장 / 같은 인증의 재요청은 기존 결과만
 */
class MemberJoinServiceTest {

    private static final String EMAIL = "newbie@example.com";
    private static final String CODE = "123456";
    private static final String PASSWORD = "pass1234";

    private final EmailCertificationService emailCertificationService = mock(EmailCertificationService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final TermsService termsService = mock(TermsService.class);
    private final FieldCryptoService crypto = cryptoService();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final EmailCertProperties properties = new EmailCertProperties();
    private final SnsSignupTokenManager snsSignupTokenManager = new SnsSignupTokenManager(crypto);
    private final MemberJoinService service = new MemberJoinService(emailCertificationService, memberService,
            termsService, crypto, passwordEncoder, properties, snsSignupTokenManager);

    private final MemberResponse memberResponse = new MemberResponse(EMAIL, "냥멍12345678", "01012345678", null,
            JoinPath.EMAIL.getCode(), false, List.of(), null, null);
    private final Map<Long, Boolean> agreements = Map.of(1L, true, 4L, false);

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(properties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private MemberCreateRequest request(String pswd, String pswdCnfrm, String mblTelno) {
        return new MemberCreateRequest(JoinPath.EMAIL.getCode(), 3L, CODE, null, mblTelno, pswd, pswdCnfrm,
                List.of(new TermsAgreeRequest(1L, true)), "req-1");
    }

    private EmailCertification verifiedCert(LocalDateTime usedAt) {
        EmailCertification cert = new EmailCertification();
        cert.setEmlCertSn(3L);
        cert.setCertTrgtEmlEnc(crypto.encrypt(EMAIL));
        cert.setCertTrgtEmlHmac(crypto.hmac(EMAIL));
        cert.setCertCmptnDt(LocalDateTime.now().minusMinutes(1));
        cert.setCertUseCmptnDt(usedAt);
        return cert;
    }

    private Member created(long mbrSn) {
        Member member = new Member();
        member.setMbrSn(mbrSn);
        member.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        member.setJoinPathCd(JoinPath.EMAIL.getCode());
        return member;
    }

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private void givenReadyToJoin() {
        when(emailCertificationService.checkForSignup(3L, CODE)).thenReturn(verifiedCert(null));
        when(termsService.checkAgreements(any())).thenReturn(agreements);
        when(memberService.create(anyString(), anyString(), anyString())).thenReturn(created(11L));
        when(memberService.toResponse(any())).thenReturn(memberResponse);
    }

    @Test
    void 검사를_모두_통과하면_인증을_쓰고_회원과_약관을_저장한다() {
        givenReadyToJoin();

        MemberJoinResult result = service.create(request(PASSWORD, PASSWORD, "010-1234-5678"));

        assertThat(result.mbrSn()).isEqualTo(11L);
        assertThat(result.response()).isSameAs(memberResponse);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(emailCertificationService, memberService, termsService);
        order.verify(emailCertificationService).checkForSignup(3L, CODE);
        order.verify(termsService).checkAgreements(any());
        order.verify(memberService).checkEmailAvailable(crypto.hmac(EMAIL));
        order.verify(memberService).checkPhoneAvailable("01012345678");
        order.verify(emailCertificationService).use(3L);
        order.verify(memberService).create(eq(EMAIL), eq("01012345678"), hash.capture());
        order.verify(termsService).createAgreements(11L, agreements);
        assertThat(passwordEncoder.matches(PASSWORD, hash.getValue())).isTrue();
        assertThat(hash.getValue()).doesNotContain(PASSWORD);
    }

    @Test
    void 비밀번호_확인이_다르면_인증을_확인하기_전에_거절한다() {
        assertThatThrownBy(() -> service.create(request(PASSWORD, "pass12345", "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.PASSWORD_MISMATCH));
        verify(emailCertificationService, never()).checkForSignup(anyLong(), anyString());
    }

    @Test
    void 이메일_가입_경로가_아니면_거절한다() {
        MemberCreateRequest req = new MemberCreateRequest(JoinPath.KAKAO.getCode(), 3L, CODE, null, "01012345678", PASSWORD,
                PASSWORD, List.of(new TermsAgreeRequest(1L, true)), null);

        assertThatThrownBy(() -> service.create(req)).satisfies(e -> assertCode(e, ErrorCode.BAD_REQUEST));
        verify(emailCertificationService, never()).checkForSignup(anyLong(), anyString());
    }

    @Test
    void 필수_약관_미동의면_인증을_쓰지_않고_저장하지_않는다() {
        when(emailCertificationService.checkForSignup(3L, CODE)).thenReturn(verifiedCert(null));
        when(termsService.checkAgreements(any())).thenThrow(new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED));

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
        verify(emailCertificationService, never()).use(anyLong());
        verify(memberService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void 인증_확인이_안_됐으면_저장하지_않는다() {
        when(emailCertificationService.checkForSignup(3L, CODE))
                .thenThrow(new CustomException(ErrorCode.EMAIL_CERT_NOT_VERIFIED));

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_NOT_VERIFIED));
        verify(memberService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void 번호가_중복이면_인증을_쓰지_않고_저장하지_않는다() {
        givenReadyToJoin();
        doThrow(new CustomException(ErrorCode.PHONE_ALREADY_USED)).when(memberService).checkPhoneAvailable(anyString());

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));
        verify(emailCertificationService, never()).use(anyLong());
        verify(memberService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void 번호_형식이_틀리면_거절한다() {
        givenReadyToJoin();

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "02-123-4567")))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_PHONE_NUMBER));
        verify(memberService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void 그_사이_이메일이_가입됐으면_저장하지_않는다() {
        givenReadyToJoin();
        doThrow(new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED)).when(memberService)
                .checkEmailAvailable(anyString());

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
        verify(memberService, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void 이미_쓴_인증으로_다시_오면_새로_만들지_않고_기존_결과를_준다() {
        when(emailCertificationService.checkForSignup(3L, CODE))
                .thenReturn(verifiedCert(LocalDateTime.now().minusMinutes(1)));
        when(memberService.findForLogin(crypto.hmac(EMAIL))).thenReturn(created(11L));
        when(memberService.toResponse(any())).thenReturn(memberResponse);

        MemberJoinResult result = service.create(request(PASSWORD, PASSWORD, "01012345678"));

        assertThat(result.mbrSn()).isEqualTo(11L);
        verify(memberService, never()).create(anyString(), anyString(), anyString());
        verify(emailCertificationService, never()).use(anyLong());
        verify(termsService, never()).createAgreements(anyLong(), any());
    }

    @Test
    void 쓴_인증의_재요청_허용_시간이_지났으면_거절한다() {
        when(emailCertificationService.checkForSignup(3L, CODE))
                .thenReturn(verifiedCert(LocalDateTime.now().minusMinutes(31)));
        when(memberService.findForLogin(crypto.hmac(EMAIL))).thenReturn(created(11L));

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_ALREADY_USED));
    }

    @Test
    void 쓴_인증인데_정상_회원이_없으면_거절한다() {
        when(emailCertificationService.checkForSignup(3L, CODE))
                .thenReturn(verifiedCert(LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> service.create(request(PASSWORD, PASSWORD, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_CERT_ALREADY_USED));
    }

    private static final String SNS_EMAIL = "sns@example.com";
    private static final String KAKAO = JoinPath.KAKAO.getCode();

    private String snsToken(String providerCd, String normalizedMblTelno) {
        ReflectionTestUtils.setField(snsSignupTokenManager, "validMinutes", 30L);
        SnsProvider provider = SnsProvider.fromCode(providerCd);
        return snsSignupTokenManager.issue(new SnsProfile(provider, providerCd + ":acnt-1", SNS_EMAIL, null, true),
                normalizedMblTelno);
    }

    private MemberCreateRequest snsRequest(String joinPathCd, String signupTkn, String mblTelno) {
        return new MemberCreateRequest(joinPathCd, null, null, signupTkn, mblTelno, null, null,
                List.of(new TermsAgreeRequest(1L, true)), null);
    }

    private void givenReadyToJoinBySns() {
        when(termsService.checkAgreements(any())).thenReturn(agreements);
        when(memberService.createWithSns(any(), anyString())).thenReturn(created(21L));
        when(memberService.toResponse(any())).thenReturn(memberResponse);
    }

    @Test
    void SNS_가입은_토큰의_이메일로_비밀번호_없이_회원과_연동을_저장한다() {
        givenReadyToJoinBySns();

        MemberJoinResult result = service.create(snsRequest(KAKAO, snsToken(KAKAO, "01012345678"), "010-1234-5678"));

        assertThat(result.mbrSn()).isEqualTo(21L);
        ArgumentCaptor<SnsSignupClaims> claims = ArgumentCaptor.forClass(SnsSignupClaims.class);
        InOrder order = inOrder(memberService, termsService);
        order.verify(termsService).checkAgreements(any());
        order.verify(memberService).checkEmailAvailable(crypto.emailHmac(SNS_EMAIL));
        order.verify(memberService).checkPhoneAvailable("01012345678");
        order.verify(memberService).createWithSns(claims.capture(), eq("01012345678"));
        order.verify(termsService).createAgreements(21L, agreements);
        assertThat(claims.getValue().email()).isEqualTo(SNS_EMAIL);
        assertThat(claims.getValue().snsOffrCd()).isEqualTo(KAKAO);
        verify(emailCertificationService, never()).checkForSignup(anyLong(), anyString());
        verify(memberService, never()).create(anyString(), anyString(), any());
    }

    @Test
    void 토큰에_번호가_없으면_입력한_번호로_가입한다() {
        givenReadyToJoinBySns();

        service.create(snsRequest(KAKAO, snsToken(KAKAO, null), "01099998888"));

        verify(memberService).createWithSns(any(), eq("01099998888"));
    }

    @Test
    void 토큰의_번호와_다른_번호는_거절하고_저장하지_않는다() {
        givenReadyToJoinBySns();

        assertThatThrownBy(() -> service.create(snsRequest(KAKAO, snsToken(KAKAO, "01012345678"), "01099998888")))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_INPUT_VALUE));
        verify(memberService, never()).createWithSns(any(), anyString());
    }

    @Test
    void 토큰의_제공자와_가입_경로가_다르면_거절한다() {
        givenReadyToJoinBySns();

        assertThatThrownBy(
                () -> service.create(snsRequest(JoinPath.NAVER.getCode(), snsToken(KAKAO, null), "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.SNS_SIGNUP_TOKEN_INVALID));
        verify(memberService, never()).createWithSns(any(), anyString());
    }

    @Test
    void 만료되거나_엉뚱한_토큰은_거절한다() {
        ReflectionTestUtils.setField(snsSignupTokenManager, "validMinutes", -1L);
        String expired = snsSignupTokenManager.issue(
                new SnsProfile(SnsProvider.KAKAO, "acnt-1", SNS_EMAIL, null, true), null);

        assertThatThrownBy(() -> service.create(snsRequest(KAKAO, expired, "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.SNS_SIGNUP_TOKEN_INVALID));
        assertThatThrownBy(() -> service.create(snsRequest(KAKAO, "garbage", "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.SNS_SIGNUP_TOKEN_INVALID));
        verify(memberService, never()).createWithSns(any(), anyString());
    }

    @Test
    void SNS_가입에_토큰이_없거나_비밀번호_인증_필드를_보내면_거절한다() {
        String token = snsToken(KAKAO, null);
        MemberCreateRequest noToken = snsRequest(KAKAO, null, "01012345678");
        MemberCreateRequest withPassword = new MemberCreateRequest(KAKAO, null, null, token, "01012345678", PASSWORD,
                PASSWORD, List.of(new TermsAgreeRequest(1L, true)), null);
        MemberCreateRequest withCert = new MemberCreateRequest(KAKAO, 3L, CODE, token, "01012345678", null, null,
                List.of(new TermsAgreeRequest(1L, true)), null);

        for (MemberCreateRequest req : List.of(noToken, withPassword, withCert)) {
            assertThatThrownBy(() -> service.create(req)).satisfies(e -> assertCode(e, ErrorCode.BAD_REQUEST));
        }
        verify(memberService, never()).createWithSns(any(), anyString());
    }

    @Test
    void 이메일_가입에_토큰을_보내거나_필수_필드가_빠지면_거절한다() {
        MemberCreateRequest withToken = new MemberCreateRequest(JoinPath.EMAIL.getCode(), 3L, CODE, "tkn",
                "01012345678", PASSWORD, PASSWORD, List.of(new TermsAgreeRequest(1L, true)), null);
        MemberCreateRequest noPassword = new MemberCreateRequest(JoinPath.EMAIL.getCode(), 3L, CODE, null,
                "01012345678", null, null, List.of(new TermsAgreeRequest(1L, true)), null);

        assertThatThrownBy(() -> service.create(withToken)).satisfies(e -> assertCode(e, ErrorCode.BAD_REQUEST));
        assertThatThrownBy(() -> service.create(noPassword)).satisfies(e -> assertCode(e, ErrorCode.BAD_REQUEST));
        verify(emailCertificationService, never()).checkForSignup(anyLong(), anyString());
    }

    @Test
    void SNS_가입의_이메일_중복은_저장하지_않는다() {
        givenReadyToJoinBySns();
        doThrow(new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED)).when(memberService)
                .checkEmailAvailable(crypto.emailHmac(SNS_EMAIL));

        assertThatThrownBy(() -> service.create(snsRequest(KAKAO, snsToken(KAKAO, null), "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
        verify(memberService, never()).createWithSns(any(), anyString());
        verify(termsService, never()).createAgreements(anyLong(), any());
    }

    @Test
    void SNS_가입의_필수_약관_미동의는_저장하지_않는다() {
        when(termsService.checkAgreements(any())).thenThrow(new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED));

        assertThatThrownBy(() -> service.create(snsRequest(KAKAO, snsToken(KAKAO, null), "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.REQUIRED_TERMS_NOT_AGREED));
        verify(memberService, never()).createWithSns(any(), anyString());
    }

    @Test
    void 같은_토큰의_재요청은_토큰_발급_뒤_만든_같은_회원의_결과만_준다() {
        Member existing = created(21L);
        existing.setRegDt(LocalDateTime.now().plusSeconds(1));
        when(memberService.findBySnsAccount(KAKAO, KAKAO + ":acnt-1")).thenReturn(existing);
        when(memberService.toResponse(existing)).thenReturn(memberResponse);

        MemberJoinResult result = service.create(snsRequest(KAKAO, snsToken(KAKAO, null), "01012345678"));

        assertThat(result.mbrSn()).isEqualTo(21L);
        verify(memberService, never()).createWithSns(any(), anyString());
        verify(termsService, never()).createAgreements(anyLong(), any());
    }

    @Test
    void 토큰_발급_전에_연동된_계정이면_SNS_ACCOUNT_ALREADY_LINKED다() {
        Member old = created(5L);
        old.setRegDt(LocalDateTime.now().minusDays(3));
        when(memberService.findBySnsAccount(KAKAO, KAKAO + ":acnt-1")).thenReturn(old);

        assertThatThrownBy(() -> service.create(snsRequest(KAKAO, snsToken(KAKAO, null), "01012345678")))
                .satisfies(e -> assertCode(e, ErrorCode.SNS_ACCOUNT_ALREADY_LINKED));
        verify(memberService, never()).createWithSns(any(), anyString());
    }
}
