package com.nyangmeong.health.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.nyangmeong.health.service.CareScheduleService;

/**
 * [검사 - 회원 단위 일정 AI 도구]
 * - 새 도구 list_my_care_schedules와 기존 list_care_schedules가 함께 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareScheduleController.class,
        MyCareScheduleController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class MyCareScheduleAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";
    private static final String TOOL = "$.data.tools[?(@.name == 'list_my_care_schedules')]";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    CareScheduleService careScheduleService;

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search(anyString())).thenReturn(List.of(summary));
    }

    @Test
    void 새_도구와_기존_일정_목록_도구가_함께_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tools[*].name", hasItems("list_my_care_schedules", "list_care_schedules")))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'list_care_schedules')].call.path")
                        .value("/api/pets/{petSn}/care-schedules"));
    }

    @Test
    void 새_도구의_칸() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".kind").value("READ"))
                .andExpect(jsonPath(TOOL + ".call.method").value("GET"))
                .andExpect(jsonPath(TOOL + ".call.path").value("/api/care-schedules"))
                .andExpect(jsonPath(TOOL + ".params.petSns.in").value("query"))
                .andExpect(jsonPath(TOOL + ".params.petSns.type").value("string"))
                .andExpect(jsonPath(TOOL + ".params.petSns.pattern").value("^$|^\\d+(,\\d+)*$"))
                .andExpect(jsonPath(TOOL + ".params.fromYmd.type").value("date"))
                .andExpect(jsonPath(TOOL + ".params.toYmd.in").value("query"))
                .andExpect(jsonPath(TOOL + ".params.schdlKndCd.group").value("HLTG19"))
                .andExpect(jsonPath(TOOL + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(TOOL + ".params.size.max").value(50))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'list_my_care_schedules')]", hasSize(1)));
    }
}
