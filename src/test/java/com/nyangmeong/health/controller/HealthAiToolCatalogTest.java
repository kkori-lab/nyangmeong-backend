package com.nyangmeong.health.controller;

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
import com.nyangmeong.health.service.CareItemService;
import com.nyangmeong.health.service.CareScheduleService;
import com.nyangmeong.health.service.CheckupAttachmentService;
import com.nyangmeong.health.service.CheckupService;
import com.nyangmeong.health.service.DailyHealthRecordService;
import com.nyangmeong.health.service.EtcInspectionService;
import com.nyangmeong.health.service.HealthIndicatorService;
import com.nyangmeong.health.service.InspectionValueService;
import com.nyangmeong.health.service.ObservationService;
import com.nyangmeong.health.service.PrescriptionService;
import com.nyangmeong.health.service.RepeatPreventionService;

/**
 * [검사 - 건강 도메인 AI 도구 목록]
 * - 실제 건강 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 13 + CARD 7)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareScheduleController.class, CareItemController.class,
        DailyHealthRecordController.class, CheckupController.class, CheckupAttachmentController.class,
        PrescriptionController.class, InspectionValueController.class, EtcInspectionController.class,
        RepeatPreventionController.class, HealthIndicatorController.class, ObservationController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class HealthAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    CareScheduleService careScheduleService;
    @MockitoBean
    CareItemService careItemService;
    @MockitoBean
    DailyHealthRecordService dailyHealthRecordService;
    @MockitoBean
    CheckupService checkupService;
    @MockitoBean
    CheckupAttachmentService checkupAttachmentService;
    @MockitoBean
    PrescriptionService prescriptionService;
    @MockitoBean
    InspectionValueService inspectionValueService;
    @MockitoBean
    EtcInspectionService etcInspectionService;
    @MockitoBean
    RepeatPreventionService repeatPreventionService;
    @MockitoBean
    HealthIndicatorService healthIndicatorService;
    @MockitoBean
    ObservationService observationService;

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
    void 건강_READ_13개와_CARD_7개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(20))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("draft_care_completion", "draft_care_item",
                        "draft_care_item_update", "draft_daily_health_record", "draft_observation_cycle",
                        "draft_schedule", "draft_schedule_update",
                        "get_checkup", "get_health_indicators", "list_care_completions", "list_care_items",
                        "list_care_schedules", "list_checkups", "list_daily_health_records",
                        "list_etc_inspections", "list_inspection_values", "list_observation_cycles",
                        "list_observation_items", "list_prescriptions", "list_repeat_preventions")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(13)));
    }

    @Test
    void 일정_목록_도구의_칸() throws Exception {
        givenCodes();
        String t = tool("list_care_schedules");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/care-schedules"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSn.required").value(true))
                .andExpect(jsonPath(t + ".params.fromYmd.type").value("date"))
                .andExpect(jsonPath(t + ".params.toYmd.in").value("query"))
                .andExpect(jsonPath(t + ".params.schdlKndCd.group").value("HLTG19"))
                .andExpect(jsonPath(t + ".params.schdlKndCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(t + ".params.size.max").value(50));
    }

    @Test
    void 관찰_기록_도구는_petSn이_경로_하나고_관찰_항목이_필수다() throws Exception {
        givenCodes();
        String t = tool("list_observation_cycles");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.obsrvArtclSn.in").value("query"))
                .andExpect(jsonPath(t + ".params.obsrvArtclSn.required").value(true));
    }

    @Test
    void 검진_목록_도구는_page와_size가_있다() throws Exception {
        givenCodes();
        String t = tool("list_checkups");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.page.in").value("query"))
                .andExpect(jsonPath(t + ".params.size.max").value(50))
                .andExpect(jsonPath(t + ".params.year.min").value(1900))
                .andExpect(jsonPath(t + ".params.ckupTypeCd.group").value("HLTG01"));
    }
}
