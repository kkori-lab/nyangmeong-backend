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
 * - 실제 건강 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 12 + CARD 9)
 * - 반려동물 1마리 일정 목록은 도구가 아님 : 회원 단위 list_my_care_schedules 가 대신 (MyCareScheduleAiToolCatalogTest)
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
    void 건강_READ_12개와_CARD_9개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(21))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("draft_care_completion", "draft_care_item",
                        "draft_care_item_update", "draft_daily_health_record", "draft_inspection_values",
                        "draft_observation_cycle", "draft_observation_item",
                        "draft_schedule", "draft_schedule_update",
                        "get_checkup", "get_health_indicators", "list_care_completions", "list_care_items",
                        "list_checkups", "list_daily_health_records",
                        "list_etc_inspections", "list_inspection_values", "list_observation_cycles",
                        "list_observation_items", "list_prescriptions", "list_repeat_preventions")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(12)));
    }

    @Test
    void 반려동물_1마리_일정_목록은_도구로_싣지_않는다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("list_care_schedules"), hasSize(0)))
                .andExpect(jsonPath("$.data.tools[?(@.call.path == '/api/pets/{petSn}/care-schedules' "
                        + "&& @.call.method == 'GET')]", hasSize(0)));
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

    @Test
    void 검사_수치_저장_도구는_검진기록_번호와_행_목록이_필수고_행_칸_규칙이_실린다() throws Exception {
        givenCodes();
        String t = tool("draft_inspection_values");
        String v = t + ".params.values.items";
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/inspection-values"))
                .andExpect(jsonPath(t + ".params.ckupRcdSn.type").value("long"))
                .andExpect(jsonPath(t + ".params.ckupRcdSn.required").value(true))
                .andExpect(jsonPath(t + ".params.values.type").value("array"))
                .andExpect(jsonPath(t + ".params.values.required").value(true))
                .andExpect(jsonPath(t + ".params.values.minItems").value(1))
                .andExpect(jsonPath(v + ".inspArtclCd.type").value("code"))
                .andExpect(jsonPath(v + ".inspArtclCd.group").value("HLTG12"))
                .andExpect(jsonPath(v + ".inspArtclCd.maxLength").value(8))
                .andExpect(jsonPath(v + ".inspArtclCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(v + ".inspArtclCd.required").doesNotExist())
                .andExpect(jsonPath(v + ".inspArtclDrctInptNm.maxLength").value(40))
                .andExpect(jsonPath(v + ".rsltNvl.type").value("decimal"))
                .andExpect(jsonPath(v + ".rsltNvl.required").value(true))
                .andExpect(jsonPath(v + ".rsltNvl.min").value(0))
                .andExpect(jsonPath(v + ".rsltNvl.precision").value(9))
                .andExpect(jsonPath(v + ".rsltNvl.scale").value(2))
                .andExpect(jsonPath(v + ".unitNm.required").value(true))
                .andExpect(jsonPath(v + ".unitNm.maxLength").value(20))
                .andExpect(jsonPath(v + ".refLlmtNvl.type").value("decimal"))
                .andExpect(jsonPath(v + ".refLlmtNvl.min").value(0))
                .andExpect(jsonPath(v + ".refLlmtNvl.scale").value(2))
                .andExpect(jsonPath(v + ".refUplmtNvl.min").value(0))
                .andExpect(jsonPath(v + ".refUplmtNvl.required").doesNotExist())
                .andExpect(jsonPath(v + ".memoCn.maxLength").value(200));
    }
    @Test
    void 관찰_항목_등록_도구는_multipart이고_부위_코드와_사진이_필수다() throws Exception {
        givenCodes();
        String t = tool("draft_observation_item");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/observation-items"))
                .andExpect(jsonPath(t + ".call.contentType").value("multipart/form-data"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.obsrvArtclNm.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvArtclNm.maxLength").value(40))
                .andExpect(jsonPath(t + ".params.obsrvRegnCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.obsrvRegnCd.group").value("HLTG16"))
                .andExpect(jsonPath(t + ".params.obsrvRegnCd.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvRegnCd.maxLength").value(8))
                .andExpect(jsonPath(t + ".params.obsrvRegnCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.obsrvRegnDtlNm.maxLength").value(40))
                .andExpect(jsonPath(t + ".params.file.type").value("file"))
                .andExpect(jsonPath(t + ".params.file.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvDt.type").value("datetime"))
                .andExpect(jsonPath(t + ".params.obsrvDt.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvSz.type").value("decimal"))
                .andExpect(jsonPath(t + ".params.obsrvSz.scale").value(2))
                .andExpect(jsonPath(t + ".params.memoCn.maxLength").value(200));
    }
}
