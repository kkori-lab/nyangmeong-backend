package com.nyangmeong.support.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.nyangmeong.support.service.FaqService;

/**
 * [검사 - 고객센터 도메인 AI 도구 목록]
 * - 실제 고객센터 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 1)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, FaqController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class SupportAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    FaqService faqService;

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
    void 고객센터_READ_1개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(1))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("list_faqs")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(1)))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'support')].name", hasSize(1)));
    }

    @Test
    void FAQ_도구는_분류_코드와_검색어와_페이지가_있다() throws Exception {
        givenCodes();
        String t = tool("list_faqs");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/faqs"))
                .andExpect(jsonPath(t + ".params.faqClsfCd.group").value("CSCG01"))
                .andExpect(jsonPath(t + ".params.keyword.type").value("string"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"));
    }
}
