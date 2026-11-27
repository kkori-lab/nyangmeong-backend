package com.nyangmeong.health.controller;

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
import com.nyangmeong.health.service.CareItemService;
import com.nyangmeong.health.service.CheckupAttachmentService;
import com.nyangmeong.health.service.HealthDashboardWidgetService;

/**
 * [검사 - 건강 도메인 AI 도구 목록 추가분]
 * - 사이트 기능 전체 표의 읽기 후보에 붙인 @AiTool이 실리는지 (READ 3)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, HealthDashboardWidgetController.class,
        CareItemController.class, CheckupAttachmentController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class HealthReadAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    HealthDashboardWidgetService healthDashboardWidgetService;
    @MockitoBean
    CareItemService careItemService;
    @MockitoBean
    CheckupAttachmentService checkupAttachmentService;

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
                .andExpect(jsonPath(tool("get_health_dashboard_widgets") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_health_dashboard_widgets") + ".domain").value("health"))
                .andExpect(jsonPath(tool("get_care_mission_progress") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_care_mission_progress") + ".domain").value("health"))
                .andExpect(jsonPath(tool("list_checkup_attachments") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_checkup_attachments") + ".domain").value("health"));
    }

    @Test
    void get_health_dashboard_widgets_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_health_dashboard_widgets");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/members/me/dashboard-widgets"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void get_care_mission_progress_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_care_mission_progress");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/care-items/mission-progress"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void list_checkup_attachments_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_checkup_attachments");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/checkup-attachments"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSn.required").value(true))
                .andExpect(jsonPath(t + ".params.ckupRcdSn.in").value("query"))
                .andExpect(jsonPath(t + ".params.ckupRcdSn.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.atchTypeCd.in").value("query"))
                .andExpect(jsonPath(t + ".params.atchTypeCd.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.atchTypeCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.atchTypeCd.group").value("HLTG20"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"))
                .andExpect(jsonPath(t + ".params.page.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.size.in").value("query"))
                .andExpect(jsonPath(t + ".params.size.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(5)));
    }
}
