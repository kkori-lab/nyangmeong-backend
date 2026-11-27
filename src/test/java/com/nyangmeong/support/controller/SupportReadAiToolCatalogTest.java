package com.nyangmeong.support.controller;

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
import com.nyangmeong.support.service.InquiryService;
import com.nyangmeong.support.service.NoticeService;

/**
 * [검사 - 고객센터 도메인 AI 도구 목록 추가분]
 * - 사이트 기능 전체 표의 읽기 후보에 붙인 @AiTool이 실리는지 (READ 4)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, InquiryController.class, NoticeController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class SupportReadAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    InquiryService inquiryService;
    @MockitoBean
    NoticeService noticeService;

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
                .andExpect(jsonPath(tool("list_inquiries") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_inquiries") + ".domain").value("support"))
                .andExpect(jsonPath(tool("get_inquiry") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_inquiry") + ".domain").value("support"))
                .andExpect(jsonPath(tool("list_notices") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_notices") + ".domain").value("support"))
                .andExpect(jsonPath(tool("get_notice") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_notice") + ".domain").value("support"));
    }

    @Test
    void list_inquiries_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_inquiries");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/inquiries"))
                .andExpect(jsonPath(t + ".params.inqryPrcsSttsCd.in").value("query"))
                .andExpect(jsonPath(t + ".params.inqryPrcsSttsCd.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.inqryPrcsSttsCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.inqryPrcsSttsCd.group").value("CSCG03"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"))
                .andExpect(jsonPath(t + ".params.page.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.size.in").value("query"))
                .andExpect(jsonPath(t + ".params.size.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(3)));
    }

    @Test
    void get_inquiry_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_inquiry");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/inquiries/{otoInqrySn}"))
                .andExpect(jsonPath(t + ".params.otoInqrySn.in").value("path"))
                .andExpect(jsonPath(t + ".params.otoInqrySn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void list_notices_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_notices");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/notices"))
                .andExpect(jsonPath(t + ".params.keyword.in").value("query"))
                .andExpect(jsonPath(t + ".params.keyword.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.latestYn.in").value("query"))
                .andExpect(jsonPath(t + ".params.latestYn.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.latestYn.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"))
                .andExpect(jsonPath(t + ".params.page.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.size.in").value("query"))
                .andExpect(jsonPath(t + ".params.size.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(4)));
    }

    @Test
    void get_notice_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_notice");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/notices/{ntcMttrSn}"))
                .andExpect(jsonPath(t + ".params.ntcMttrSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.ntcMttrSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }
}
