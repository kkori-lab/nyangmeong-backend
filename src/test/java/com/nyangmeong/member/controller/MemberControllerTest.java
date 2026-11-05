package com.nyangmeong.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.dto.MemberCreateRequest;
import com.nyangmeong.member.dto.MemberJoinResult;
import com.nyangmeong.member.dto.MemberNicknameAvailabilityResponse;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberPhoneAvailabilityResponse;
import com.nyangmeong.member.dto.MemberRecoverRequest;
import com.nyangmeong.member.dto.MemberRecoveryInfoRequest;
import com.nyangmeong.member.dto.MemberRecoveryResponse;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberUpdateRequest;
import com.nyangmeong.member.dto.MemberWithdrawRequest;
import com.nyangmeong.member.dto.PasswordChangeRequest;
import com.nyangmeong.member.dto.PasswordVerifyRequest;
import com.nyangmeong.member.service.MemberJoinService;
import com.nyangmeong.member.service.MemberPasswordService;
import com.nyangmeong.member.service.MemberRecoveryService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberWithdrawService;
import com.nyangmeong.member.service.NicknameSetupTokenManager;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - 회원 API]
 * - 가입은 로그인 쿠키 없이 201 + 닉네임 설정 쿠키만 / 입력 오류는 서비스까지 가지 않는다
 * - 닉네임 설정은 로그인 회원 또는 닉네임 설정 쿠키만 / 둘 다 없으면 401
 * - 쿠키는 실제 NicknameSetupTokenManager 로 발급·검증한다
 * - 비밀번호 확인·변경은 로그인 회원만 / 필수값·형식 오류는 서비스까지 가지 않고 / 변경 성공 시 요청한 기기에 새 세션을 준다
 */
@WebMvcTest(controllers = MemberController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, NicknameSetupTokenManager.class,
        FieldCryptoService.class, CryptoProperties.class})
class MemberControllerTest {

    private static final String JOIN_BODY = """
            {"joinPathCd":"USRC0006","emlCertSn":3,"certNo":"123456","mblTelno":"010-1234-5678",
             "pswd":"pass1234","pswdCnfrm":"pass1234",
             "trmsAgres":[{"trmsSn":1,"agreYn":true},{"trmsSn":4,"agreYn":false}],"idmptDmndId":"req-1"}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    NicknameSetupTokenManager nicknameSetupTokenManager;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    MemberJoinService memberJoinService;

    @MockitoBean
    MemberRecoveryService memberRecoveryService;

    @MockitoBean
    MemberPasswordService memberPasswordService;

    @MockitoBean
    MemberWithdrawService memberWithdrawService;

    @MockitoBean
    LoginSessionManager loginSessionManager;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MemberResponse memberResponse = new MemberResponse("newbie@example.com", "냥멍12345678",
            "01012345678", null, "USRC0006", false, List.of(), null, null);

    private MockHttpServletRequestBuilder join(String body) {
        return post("/api/members")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder nickname(String body) {
        return patch("/api/members/me/nickname")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private Cookie setupCookie(long mbrSn) {
        return new Cookie(NicknameSetupTokenManager.COOKIE_NAME, nicknameSetupTokenManager.issue(mbrSn).getValue());
    }

    @Test
    void 가입은_201과_닉네임_설정_쿠키만_준다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenReturn(new MemberJoinResult(11L, memberResponse));

        mockMvc.perform(join(JOIN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.mbrNcnm").value("냥멍12345678"))
                .andExpect(jsonPath("$.data.ncnmStngCmptnYn").value(false))
                .andExpect(header().stringValues("Set-Cookie", Matchers.contains(
                        Matchers.allOf(Matchers.startsWith("ncnm_setup="), Matchers.containsString("HttpOnly"),
                                Matchers.containsString("Path=/api/members")))))
                .andExpect(header().string("Set-Cookie", Matchers.not(Matchers.containsString("access_token"))));
    }

    @Test
    void 요청_헤더가_없으면_가입은_403이다() throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(JOIN_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void 비밀번호_형식이_틀리면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(join(JOIN_BODY.replace("pass1234", "password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[?(@.field=='pswd')].message")
                        .value("비밀번호는 8자 이상, 영문과 숫자를 함께 입력해 주세요."));

        verify(memberJoinService, never()).create(any());
    }

    @Test
    void 약관_동의_내용이_비어_있으면_400이다() throws Exception {
        mockMvc.perform(join("""
                {"joinPathCd":"USRC0006","emlCertSn":3,"certNo":"123456","mblTelno":"01012345678",
                 "pswd":"pass1234","pswdCnfrm":"pass1234","trmsAgres":[]}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("trmsAgres"));

        verify(memberJoinService, never()).create(any());
    }

    @Test
    void 번호가_중복이면_409이다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.PHONE_ALREADY_USED));

        mockMvc.perform(join(JOIN_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_USED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void 번호_중복확인은_로그인_없이_정규화한_번호와_가능_여부를_준다() throws Exception {
        when(memberService.getPhoneAvailability(eq("010-1234-5678"), isNull()))
                .thenReturn(new MemberPhoneAvailabilityResponse("01012345678", true));

        mockMvc.perform(get("/api/members/phone-availability").param("mblTelno", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.normalizedMblTelno").value("01012345678"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 로그인_회원이_번호를_확인하면_본인_번호로_판정한다() throws Exception {
        when(memberService.getPhoneAvailability("01012345678", 5L))
                .thenReturn(new MemberPhoneAvailabilityResponse("01012345678", true));

        mockMvc.perform(get("/api/members/phone-availability").param("mblTelno", "01012345678"))
                .andExpect(status().isOk());

        verify(memberService).getPhoneAvailability("01012345678", 5L);
    }

    @Test
    void 번호가_비어_있으면_400이다() throws Exception {
        mockMvc.perform(get("/api/members/phone-availability").param("mblTelno", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("휴대전화번호를 입력해 주세요."));
    }

    @Test
    void 닉네임_중복확인은_로그인_없이_가능_여부를_준다() throws Exception {
        when(memberService.getNicknameAvailability("초코맘", null))
                .thenReturn(new MemberNicknameAvailabilityResponse(true));

        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초코맘"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void 닉네임_설정_쿠키가_있으면_그_회원_기준으로_중복을_판정한다() throws Exception {
        when(memberService.getNicknameAvailability("초코맘", 11L))
                .thenReturn(new MemberNicknameAvailabilityResponse(true));

        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초코맘").cookie(setupCookie(11L)))
                .andExpect(status().isOk());

        verify(memberService).getNicknameAvailability("초코맘", 11L);
    }

    @Test
    void 닉네임이_열한_글자면_400이고_문구는_화면설계서와_같다() throws Exception {
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "가나다라마바사아자차카"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("닉네임은 최대 10글자까지 입력할 수 있습니다."));
    }

    @Test
    void 닉네임에_특수문자와_공백이_있으면_400이다() throws Exception {
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초코!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다."));
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초 코"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 닉네임이_비어_있으면_400이다() throws Exception {
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("닉네임을 입력해 주세요."));
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[*].message", Matchers.hasItem("닉네임을 입력해 주세요.")));
    }

    @Test
    void 로그인도_쿠키도_없으면_닉네임_설정은_401이다() throws Exception {
        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(memberService, never()).updateNickname(any(), any());
    }

    @Test
    void 닉네임_설정_쿠키가_있으면_그_회원의_닉네임을_저장한다() throws Exception {
        when(memberService.updateNickname(eq(11L), any(MemberNicknameUpdateRequest.class)))
                .thenReturn(new MemberResponse("newbie@example.com", "초코맘", "01012345678", null, "USRC0006", true,
                        List.of(), null, null));

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}").cookie(setupCookie(11L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mbrNcnm").value("초코맘"))
                .andExpect(jsonPath("$.data.ncnmStngCmptnYn").value(true))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void 위조한_쿠키로는_닉네임을_저장할_수_없다() throws Exception {
        Cookie forged = new Cookie(NicknameSetupTokenManager.COOKIE_NAME, "99.9999999999." + "0".repeat(64));

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}").cookie(forged))
                .andExpect(status().isUnauthorized());

        verify(memberService, never()).updateNickname(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 로그인_회원은_쿠키_없이_닉네임을_저장한다() throws Exception {
        when(memberService.updateNickname(eq(5L), any(MemberNicknameUpdateRequest.class))).thenReturn(memberResponse);

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}"))
                .andExpect(status().isOk());

        verify(memberService).updateNickname(eq(5L), any(MemberNicknameUpdateRequest.class));
    }

    @Test
    void 닉네임이_중복이면_409이다() throws Exception {
        when(memberService.updateNickname(eq(11L), any(MemberNicknameUpdateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.NICKNAME_ALREADY_USED));

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}").cookie(setupCookie(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요."));
    }

    @Test
    void 닉네임_형식이_틀리면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘!!\"}").cookie(setupCookie(11L)))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).updateNickname(any(), any());
    }

    private MockHttpServletRequestBuilder recovery(String path, String body) {
        return post(path)
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void 복구_안내는_로그인_쿠키_없이_200이고_보관_정보를_준다() throws Exception {
        when(memberRecoveryService.getRecovery(any(MemberRecoveryInfoRequest.class)))
                .thenReturn(new MemberRecoveryResponse("newbie@example.com", "초코맘",
                        LocalDateTime.of(2026, 10, 20, 9, 0), LocalDateTime.of(2026, 11, 19, 9, 0), "010-****-5678",
                        List.of()));

        mockMvc.perform(recovery("/api/members/recovery-info", "{\"emlCertSn\":3,\"rcvryAuthrtTkn\":\"tkn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acntEml").value("newbie@example.com"))
                .andExpect(jsonPath("$.data.mbrNcnm").value("초코맘"))
                .andExpect(jsonPath("$.data.maskedMblTelno").value("010-****-5678"))
                .andExpect(jsonPath("$.data.missingTrms").isEmpty());
    }

    @Test
    void 보관_번호가_없으면_복구_안내에서_가린_번호가_빠진다() throws Exception {
        when(memberRecoveryService.getRecovery(any(MemberRecoveryInfoRequest.class)))
                .thenReturn(new MemberRecoveryResponse("newbie@example.com", "초코맘",
                        LocalDateTime.of(2026, 10, 20, 9, 0), LocalDateTime.of(2026, 11, 19, 9, 0), null, List.of()));

        mockMvc.perform(recovery("/api/members/recovery-info", "{\"emlCertSn\":3,\"rcvryAuthrtTkn\":\"tkn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedMblTelno").doesNotExist());
    }

    @Test
    void 복구_안내는_값이_비면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(recovery("/api/members/recovery-info", "{\"emlCertSn\":3,\"rcvryAuthrtTkn\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(recovery("/api/members/recovery-info", "{\"rcvryAuthrtTkn\":\"tkn\"}"))
                .andExpect(status().isBadRequest());

        verify(memberRecoveryService, never()).getRecovery(any());
    }

    @Test
    void 복구_권한이_없으면_400이다() throws Exception {
        when(memberRecoveryService.getRecovery(any(MemberRecoveryInfoRequest.class)))
                .thenThrow(new CustomException(ErrorCode.MEMBER_RECOVERY_NOT_AVAILABLE));

        mockMvc.perform(recovery("/api/members/recovery-info", "{\"emlCertSn\":3,\"rcvryAuthrtTkn\":\"tkn\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("복구 확인 시간이 지났습니다. 이메일 인증을 다시 진행해 주세요."));
    }

    @Test
    void 복구_실행은_로그인_쿠키_없이_200이고_쿠키를_내리지_않는다() throws Exception {
        mockMvc.perform(recovery("/api/members/recovery",
                "{\"emlCertSn\":3,\"rcvryAuthrtTkn\":\"tkn\",\"trmsAgres\":[{\"trmsSn\":1,\"agreYn\":true}],"
                        + "\"mblTelno\":\"010-1234-5678\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));

        verify(memberRecoveryService).recover(any(MemberRecoverRequest.class));
    }

    @Test
    void 복구_실행은_약관_항목이_틀리거나_값이_비면_400이다() throws Exception {
        mockMvc.perform(recovery("/api/members/recovery",
                "{\"emlCertSn\":3,\"rcvryAuthrtTkn\":\"tkn\",\"trmsAgres\":[{\"trmsSn\":1}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(recovery("/api/members/recovery", "{\"emlCertSn\":3}"))
                .andExpect(status().isBadRequest());

        verify(memberRecoveryService, never()).recover(any());
    }

    @Test
    void 복구_요청_문자열에_권한_토큰이_남지_않는다() {
        assertThat(new MemberRecoveryInfoRequest(3L, "secret-tkn").toString()).doesNotContain("secret-tkn");
        assertThat(new MemberRecoverRequest(3L, "secret-tkn", null, "01012345678").toString())
                .doesNotContain("secret-tkn", "01012345678");
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 회원정보_수정_조회는_로그인_회원의_정보를_준다() throws Exception {
        when(memberService.getMember(5L)).thenReturn(new MemberResponse("newbie@example.com", "초코맘", "01012345678",
                null, "USRC0006", true, List.of(), LocalDateTime.of(2026, 8, 14, 9, 0),
                LocalDateTime.of(2026, 10, 21, 10, 42)));

        mockMvc.perform(get("/api/members/me").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acntEml").value("newbie@example.com"))
                .andExpect(jsonPath("$.data.mblTelno").value("01012345678"))
                .andExpect(jsonPath("$.data.joinDt").exists())
                .andExpect(jsonPath("$.data.lastLgnDt").exists());
    }

    @Test
    void 회원정보_수정_조회는_로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(get("/api/members/me").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());

        verify(memberService, never()).getMember(any());
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 회원정보_수정은_multipart로_번호와_사진을_받는다() throws Exception {
        when(memberService.updateMember(eq(5L), any(MemberUpdateRequest.class))).thenReturn(memberResponse);
        MockMultipartFile photo = new MockMultipartFile("profilePhoto", "me.png", "image/png", new byte[] {1, 2});

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/members/me").file(photo)
                .param("mblTelno", "+82 10-1234-5678")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk());

        ArgumentCaptor<MemberUpdateRequest> captor = ArgumentCaptor.forClass(MemberUpdateRequest.class);
        verify(memberService).updateMember(eq(5L), captor.capture());
        assertThat(captor.getValue().mblTelno()).isEqualTo("+82 10-1234-5678");
        assertThat(captor.getValue().profilePhoto().getOriginalFilename()).isEqualTo("me.png");
    }

    @Test
    void 회원정보_수정은_로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/members/me").param("profilePhotoReset", "true")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());

        verify(memberService, never()).updateMember(any(), any());
    }

    private MockHttpServletRequestBuilder verifyPassword(String body) {
        return post("/api/members/me/password-verification")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder changePassword(String body) {
        return patch("/api/members/me/password")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_확인은_통과하면_200이다() throws Exception {
        mockMvc.perform(verifyPassword("{\"pswd\":\"oldpass12\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<PasswordVerifyRequest> captor = ArgumentCaptor.forClass(PasswordVerifyRequest.class);
        verify(memberPasswordService).verify(eq(5L), captor.capture());
        assertThat(captor.getValue().pswd()).isEqualTo("oldpass12");
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_확인에서_현재_비밀번호가_틀리면_400이다() throws Exception {
        org.mockito.Mockito.doThrow(new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH))
                .when(memberPasswordService).verify(eq(5L), any(PasswordVerifyRequest.class));

        mockMvc.perform(verifyPassword("{\"pswd\":\"wrongpass1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"));
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_확인은_비밀번호가_비면_400이고_서비스까지_가지_않는다() throws Exception {
        mockMvc.perform(verifyPassword("{}")).andExpect(status().isBadRequest());
        mockMvc.perform(verifyPassword("{\"pswd\":\"  \"}")).andExpect(status().isBadRequest());

        verify(memberPasswordService, never()).verify(any(), any());
    }

    @Test
    void 비밀번호_확인은_로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(verifyPassword("{\"pswd\":\"oldpass12\"}"))
                .andExpect(status().isUnauthorized());

        verify(memberPasswordService, never()).verify(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_변경은_바꾼_뒤_요청한_기기에_새_세션을_준다() throws Exception {
        mockMvc.perform(changePassword(
                "{\"currentPswd\":\"oldpass12\",\"pswd\":\"newpass34\",\"pswdCnfrm\":\"newpass34\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<PasswordChangeRequest> captor = ArgumentCaptor.forClass(PasswordChangeRequest.class);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(memberPasswordService, loginSessionManager);
        order.verify(memberPasswordService).change(eq(5L), captor.capture());
        order.verify(loginSessionManager).issue(eq(5L), any());
        assertThat(captor.getValue().currentPswd()).isEqualTo("oldpass12");
        assertThat(captor.getValue().pswd()).isEqualTo("newpass34");
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_변경은_세_값_중_하나라도_비면_400이다() throws Exception {
        mockMvc.perform(changePassword("{\"pswd\":\"newpass34\",\"pswdCnfrm\":\"newpass34\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(changePassword("{\"currentPswd\":\"oldpass12\",\"pswdCnfrm\":\"newpass34\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(changePassword("{\"currentPswd\":\"oldpass12\",\"pswd\":\"newpass34\"}"))
                .andExpect(status().isBadRequest());

        verify(memberPasswordService, never()).change(any(), any());
        verify(loginSessionManager, never()).issue(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_변경은_새_비밀번호_형식이_틀리면_400이다() throws Exception {
        for (String weak : new String[] {"short1", "onlyletters", "12345678", "has space1a"}) {
            mockMvc.perform(changePassword("{\"currentPswd\":\"oldpass12\",\"pswd\":\"" + weak
                    + "\",\"pswdCnfrm\":\"" + weak + "\"}")).andExpect(status().isBadRequest());
        }

        verify(memberPasswordService, never()).change(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 비밀번호_변경이_거절되면_새_세션을_주지_않는다() throws Exception {
        org.mockito.Mockito.doThrow(new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH))
                .when(memberPasswordService).change(eq(5L), any(PasswordChangeRequest.class));

        mockMvc.perform(changePassword(
                "{\"currentPswd\":\"wrongpass1\",\"pswd\":\"newpass34\",\"pswdCnfrm\":\"newpass34\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"));

        verify(loginSessionManager, never()).issue(any(), any());
    }

    @Test
    void 비밀번호_변경은_로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(changePassword(
                "{\"currentPswd\":\"oldpass12\",\"pswd\":\"newpass34\",\"pswdCnfrm\":\"newpass34\"}"))
                .andExpect(status().isUnauthorized());

        verify(memberPasswordService, never()).change(any(), any());
    }

    private static final String SNS_JOIN_BODY = """
            {"joinPathCd":"USRC0008","signupTkn":"tkn","mblTelno":"01012345678",
             "trmsAgres":[{"trmsSn":1,"agreYn":true}]}
            """;

    @Test
    void SNS_가입은_signupTkn을_서비스에_넘기고_201과_닉네임_쿠키를_준다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenReturn(new MemberJoinResult(21L, memberResponse));

        mockMvc.perform(join(SNS_JOIN_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", Matchers.startsWith("ncnm_setup=")));

        ArgumentCaptor<MemberCreateRequest> captor = ArgumentCaptor.forClass(MemberCreateRequest.class);
        verify(memberJoinService).create(captor.capture());
        assertThat(captor.getValue().signupTkn()).isEqualTo("tkn");
        assertThat(captor.getValue().pswd()).isNull();
    }

    @Test
    void SNS_가입에_pswd를_보내면_400이다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.BAD_REQUEST));

        mockMvc.perform(join(SNS_JOIN_BODY.replace("\"signupTkn\":\"tkn\",",
                "\"signupTkn\":\"tkn\",\"pswd\":\"pass1234\",\"pswdCnfrm\":\"pass1234\",")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void SNS_가입에_signupTkn이_없으면_400이다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.BAD_REQUEST));

        mockMvc.perform(join(SNS_JOIN_BODY.replace("\"signupTkn\":\"tkn\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private MockHttpServletRequestBuilder withdraw(String body) {
        return post("/api/members/me/withdraw")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 회원탈퇴는_탈퇴한_뒤_요청한_기기의_쿠키를_지운다() throws Exception {
        mockMvc.perform(withdraw("{\"noticeConfirmed\":true,\"confirmPhrase\":\"정말로 탈퇴하겠습니다.\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<MemberWithdrawRequest> captor = ArgumentCaptor.forClass(MemberWithdrawRequest.class);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(memberWithdrawService, loginSessionManager);
        order.verify(memberWithdrawService).withdraw(eq(5L), captor.capture());
        order.verify(loginSessionManager).revoke(any(), any());
        assertThat(captor.getValue().confirmPhrase()).isEqualTo("정말로 탈퇴하겠습니다.");
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 회원탈퇴는_유의사항_확인이_없거나_false면_400이다() throws Exception {
        mockMvc.perform(withdraw("{\"noticeConfirmed\":false,\"confirmPhrase\":\"정말로 탈퇴하겠습니다.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        mockMvc.perform(withdraw("{\"confirmPhrase\":\"정말로 탈퇴하겠습니다.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        mockMvc.perform(withdraw("{\"noticeConfirmed\":true}"))
                .andExpect(status().isBadRequest());

        verify(memberWithdrawService, never()).withdraw(any(), any());
        verify(loginSessionManager, never()).revoke(any(), any());
    }

    @Test
    void 회원탈퇴는_로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(withdraw("{\"noticeConfirmed\":true,\"confirmPhrase\":\"정말로 탈퇴하겠습니다.\"}"))
                .andExpect(status().isUnauthorized());

        verify(memberWithdrawService, never()).withdraw(any(), any());
    }
}
