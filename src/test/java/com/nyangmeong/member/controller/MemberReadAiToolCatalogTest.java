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
 * [검사 - 회원 도메인 AI 도구 목록 추가분]
 * - 사이트 기능 전체 표의 읽기 후보에 붙인 @AiTool이 실리는지 (READ 3)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, MemberController.class, TermsController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class MemberReadAiToolCatalogTest {

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
    void 새_도구가_종류와_도메인과_함께_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("get_service_tutorial") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_service_tutorial") + ".domain").value("member"))
                .andExpect(jsonPath(tool("list_terms") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_terms") + ".domain").value("member"))
                .andExpect(jsonPath(tool("get_terms") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_terms") + ".domain").value("member"));
    }

    @Test
    void get_service_tutorial_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_service_tutorial");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/members/me/tutorial"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void list_terms_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_terms");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/terms"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void get_terms_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_terms");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/terms/{trmsSn}"))
                .andExpect(jsonPath(t + ".params.trmsSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.trmsSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }
}
