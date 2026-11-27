package com.nyangmeong.member.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.aitool.AiToolCatalogController;
import com.nyangmeong.global.aitool.AiToolCatalogService;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.service.MemberJoinService;
import com.nyangmeong.member.service.MemberPasswordService;
import com.nyangmeong.member.service.MemberRecoveryService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSnsLinkService;
import com.nyangmeong.member.service.MemberWithdrawService;
import com.nyangmeong.member.service.NicknameSetupTokenManager;
import com.nyangmeong.member.service.TermsService;

/**
 * [검사 - 회원 AI 쓰기 카드 도구]
 * - draft_member_nickname_update : PATCH 본문 mbrNcnm 하나(필수·10자·패턴)
 * - draft_member_profile_update : PATCH multipart, 사진·기본 이미지 되돌리기 2칸, 휴대전화번호는 싣지 않음
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, MemberController.class, TermsController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class MemberCardAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
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
    MemberSnsLinkService memberSnsLinkService;
    @MockitoBean
    LoginSessionManager loginSessionManager;
    @MockitoBean
    NicknameSetupTokenManager nicknameSetupTokenManager;
    @MockitoBean
    TermsService termsService;

    private static String tool(String name) {
        return "$.data.tools[?(@.name == '" + name + "')]";
    }

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search(anyString())).thenReturn(List.of(summary));
    }

    @Test
    void 닉네임_변경_도구는_PATCH이고_본문_닉네임만_있다() throws Exception {
        givenCodes();
        String t = tool("draft_member_nickname_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("member"))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/members/me/nickname"))
                .andExpect(jsonPath(t + ".call.contentType").doesNotExist())
                .andExpect(jsonPath(t + ".params.mbrNcnm.in").value("body"))
                .andExpect(jsonPath(t + ".params.mbrNcnm.type").value("string"))
                .andExpect(jsonPath(t + ".params.mbrNcnm.required").value(true))
                .andExpect(jsonPath(t + ".params.mbrNcnm.maxLength").value(10))
                .andExpect(jsonPath(t + ".params.mbrNcnm.pattern").exists())
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void 회원정보_수정_도구는_multipart이고_휴대전화번호가_없다() throws Exception {
        givenCodes();
        String t = tool("draft_member_profile_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("member"))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/members/me"))
                .andExpect(jsonPath(t + ".call.contentType").value("multipart/form-data"))
                .andExpect(jsonPath(t + ".params.profilePhoto.type").value("file"))
                .andExpect(jsonPath(t + ".params.profilePhotoReset.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.mblTelno").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }
}
