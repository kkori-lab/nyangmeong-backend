package com.nyangmeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.nyangmeong.auth.dto.SnsLoginRequest;
import com.nyangmeong.auth.dto.SnsLoginResponse;
import com.nyangmeong.auth.dto.SnsLoginResult;
import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.FakeSnsOAuthClient;
import com.nyangmeong.global.external.SnsOAuthClient;
import com.nyangmeong.global.external.SnsProvider;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberSnsAccountSummary;
import com.nyangmeong.member.dto.SnsSignupClaims;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.SnsSignupTokenManager;

/**
 * [검사 - SNS 로그인 서비스]
 * - 연결된 회원 / 같은 이메일의 회원 / 신규 계정 분기
 * - 같은 이메일의 정상 회원은 카카오·구글이면 자동 연동하고 로그인, 네이버·정지 회원·같은 제공자 연동이 이미 있으면 중복 안내
 * - 이메일이 같다는 이유로 탈퇴 회원을 복구하지 않는다
 * - 제공자 호출은 가짜 클라이언트(fake-이름 코드)로 대신한다
 */
class SnsLoginServiceTest {

    private static final String KAKAO = SnsProvider.KAKAO.getCode();
    private static final String NAVER = SnsProvider.NAVER.getCode();
    private static final String GOOGLE = SnsProvider.GOOGLE.getCode();

    private final SnsOAuthClient snsOAuthClient = new FakeSnsOAuthClient();
    private final MemberService memberService = mock(MemberService.class);
    private final FieldCryptoService crypto = cryptoService();
    private final SnsSignupTokenManager tokenManager = new SnsSignupTokenManager(crypto);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final SnsLoginService service = new SnsLoginService(snsOAuthClient,
            new SnsLoginProcessor(memberService, tokenManager, crypto, adminTaskLogService));

    SnsLoginServiceTest() {
        ReflectionTestUtils.setField(tokenManager, "validMinutes", 30L);
    }

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(properties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private SnsLoginRequest request(String snsOffrCd, String code) {
        return new SnsLoginRequest(snsOffrCd, code, null);
    }

    private Member member(String statusCd, String ncnmStngCmptnYn, LocalDateTime lastLgnDt) {
        Member member = new Member();
        member.setMbrSn(7L);
        member.setMbrSttsCd(statusCd);
        member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
        member.setAcntEmlEnc(crypto.encrypt("choco@example.com"));
        member.setMbrNcnm("초코맘");
        member.setNcnmStngCmptnYn(ncnmStngCmptnYn);
        member.setLastLgnDt(lastLgnDt);
        return member;
    }

    private void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private void givenLinked(String code, Member member) {
        String accountId = KAKAO + ":" + code;
        when(memberService.findBySnsAccount(KAKAO, accountId)).thenReturn(member);
    }

    @Test
    void 연결된_정상_회원은_로그인하고_최종로그인을_갱신한다() {
        givenLinked("fake-choco", member(MemberStatus.NORMAL.getCode(), "Y", LocalDateTime.now().minusDays(1)));

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.loginMbrSn()).isEqualTo(7L);
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.LOGIN);
        assertThat(result.response().ncnmReguide()).isFalse();
        assertThat(result.response().mbrNcnm()).isEqualTo("초코맘");
        verify(memberService).recordLogin(7L);
    }

    @Test
    void 가입_후_첫_로그인인데_닉네임_설정이_안_끝났으면_재안내한다() {
        givenLinked("fake-choco", member(MemberStatus.NORMAL.getCode(), "N", null));

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.response().ncnmReguide()).isTrue();
    }

    @Test
    void 연결된_정지_회원은_MEMBER_INACTIVE이고_로그인하지_않는다() {
        givenLinked("fake-choco", member(MemberStatus.SUSPENDED.getCode(), "Y", LocalDateTime.now()));

        assertThatThrownBy(() -> service.login(request(KAKAO, "fake-choco")))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_INACTIVE));
        verify(memberService, never()).recordLogin(anyLong());
    }

    @Test
    void 연결된_탈퇴_보관_회원은_복구_안내이고_로그인하지_않는다() {
        givenLinked("fake-choco", member(MemberStatus.WITHDRAWN.getCode(), "Y", LocalDateTime.now()));

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.loginMbrSn()).isNull();
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.RECOVERY_AVAILABLE);
        assertThat(result.response().acntEml()).isEqualTo("choco@example.com");
        verify(memberService, never()).recordLogin(anyLong());
    }

    @Test
    void 네이버는_연결이_없고_같은_이메일의_정상_회원이_있으면_중복_안내이고_연결하지_않는다() {
        when(memberService.findForLogin(crypto.emailHmac("choco@example.com")))
                .thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", LocalDateTime.now()));

        SnsLoginResult result = service.login(request(NAVER, "fake-choco"));

        assertThat(result.loginMbrSn()).isNull();
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.EMAIL_DUPLICATE);
        assertThat(result.response().acntEml()).isEqualTo("choco@example.com");
        verify(memberService, never()).recordLogin(anyLong());
        verify(memberService, never()).createWithSns(any(), anyString());
        verify(memberService, never()).linkSns(anyLong(), anyString(), anyString());
    }

    private MemberResponse memberWithLinks(String... snsOffrCds) {
        return new MemberResponse("choco@example.com", "초코맘", "01012345678", null, "USRC0006", true,
                List.of(snsOffrCds).stream().map(MemberSnsAccountSummary::new).toList(), null, null);
    }

    @Test
    void 카카오는_같은_이메일의_정상_회원에_자동_연동하고_로그인한다() {
        when(memberService.findForLogin(crypto.emailHmac("choco@example.com")))
                .thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", LocalDateTime.now().minusDays(1)));
        when(memberService.getMember(7L)).thenReturn(memberWithLinks());

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.loginMbrSn()).isEqualTo(7L);
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.LOGIN);
        verify(memberService).linkSns(7L, KAKAO, KAKAO + ":fake-choco");
        verify(memberService).recordLogin(7L);
    }

    @Test
    void 구글도_다른_SNS만_연동된_같은_이메일의_정상_회원에_자동_연동한다() {
        when(memberService.findForLogin(crypto.emailHmac("choco@example.com")))
                .thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", LocalDateTime.now().minusDays(1)));
        when(memberService.getMember(7L)).thenReturn(memberWithLinks(KAKAO));

        SnsLoginResult result = service.login(request(GOOGLE, "fake-choco"));

        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.LOGIN);
        verify(memberService).linkSns(7L, GOOGLE, GOOGLE + ":fake-choco");
    }

    @Test
    void 같은_제공자의_다른_계정이_이미_연동돼_있으면_자동_연동하지_않고_중복_안내다() {
        when(memberService.findForLogin(crypto.emailHmac("choco@example.com")))
                .thenReturn(member(MemberStatus.NORMAL.getCode(), "Y", LocalDateTime.now()));
        when(memberService.getMember(7L)).thenReturn(memberWithLinks(KAKAO));

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.loginMbrSn()).isNull();
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.EMAIL_DUPLICATE);
        verify(memberService, never()).linkSns(anyLong(), anyString(), anyString());
        verify(memberService, never()).recordLogin(anyLong());
    }

    @Test
    void 같은_이메일의_정지_회원에는_자동_연동하지_않고_중복_안내다() {
        when(memberService.findForLogin(crypto.emailHmac("choco@example.com")))
                .thenReturn(member(MemberStatus.SUSPENDED.getCode(), "Y", LocalDateTime.now()));

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.loginMbrSn()).isNull();
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.EMAIL_DUPLICATE);
        verify(memberService, never()).linkSns(anyLong(), anyString(), anyString());
    }

    @Test
    void 연결이_없고_같은_이메일의_탈퇴_보관_회원이_있으면_복구_안내다() {
        when(memberService.findForLogin(crypto.emailHmac("choco@example.com")))
                .thenReturn(member(MemberStatus.WITHDRAWN.getCode(), "Y", LocalDateTime.now()));

        SnsLoginResult result = service.login(request(KAKAO, "fake-choco"));

        assertThat(result.loginMbrSn()).isNull();
        assertThat(result.response().snsLoginRslt()).isEqualTo(SnsLoginResponse.RECOVERY_AVAILABLE);
        verify(memberService, never()).recordLogin(anyLong());
    }

    @Test
    void 처음_보는_계정은_가입_확인_정보와_가입_토큰을_준다() {
        SnsLoginResult result = service.login(request(KAKAO, "fake-newbie"));

        SnsLoginResponse response = result.response();
        assertThat(result.loginMbrSn()).isNull();
        assertThat(response.snsLoginRslt()).isEqualTo(SnsLoginResponse.SIGNUP_REQUIRED);
        assertThat(response.snsOffrCd()).isEqualTo(KAKAO);
        assertThat(response.acntEml()).isEqualTo("newbie@example.com");
        assertThat(response.mblTelno()).matches("^01\\d{8,9}$");
        assertThat(response.ageCnfrmd()).isFalse();
        SnsSignupClaims claims = tokenManager.resolve(response.signupTkn());
        assertThat(claims.offrAcntId()).isEqualTo(KAKAO + ":fake-newbie");
        assertThat(claims.email()).isEqualTo("newbie@example.com");
        assertThat(claims.mblTelno()).isEqualTo(response.mblTelno());
        verify(memberService, never()).recordLogin(anyLong());
    }

    @Test
    void 구글은_번호를_주지_않고_연령_확인도_하지_않는다() {
        SnsLoginResponse response = service.login(request(SnsProvider.GOOGLE.getCode(), "fake-newbie")).response();

        assertThat(response.snsLoginRslt()).isEqualTo(SnsLoginResponse.SIGNUP_REQUIRED);
        assertThat(response.mblTelno()).isNull();
        assertThat(response.ageCnfrmd()).isFalse();
    }

    @Test
    void 이메일을_받지_못하면_SNS_EMAIL_NOT_PROVIDED다() {
        assertThatThrownBy(() -> service.login(request(KAKAO, "fake-newbie-noemail")))
                .satisfies(e -> assertCode(e, ErrorCode.SNS_EMAIL_NOT_PROVIDED));
    }

    @Test
    void 제공자_인증이_실패하면_SNS_AUTH_FAILED다() {
        assertThatThrownBy(() -> service.login(request(KAKAO, "real-code")))
                .satisfies(e -> assertCode(e, ErrorCode.SNS_AUTH_FAILED));
    }

    @Test
    void 이메일_가입_코드나_모르는_제공자는_거절한다() {
        assertThatThrownBy(() -> service.login(request("USRC0006", "fake-choco")))
                .satisfies(e -> assertCode(e, ErrorCode.BAD_REQUEST));
        assertThatThrownBy(() -> service.login(request("USRC0099", "fake-choco")))
                .isInstanceOf(CustomException.class);
    }
}
