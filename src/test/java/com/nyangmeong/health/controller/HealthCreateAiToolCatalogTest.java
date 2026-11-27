package com.nyangmeong.health.controller;

import static org.hamcrest.Matchers.hasItems;
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
 * [검사 - 건강 등록·종료 AI 카드 도구]
 * - draft_checkup·draft_prescription·draft_etc_inspection·draft_repeat_prevention : 칸·필수·코드·조건부 규칙
 * - end_observation_item·cancel_last_dose : 본문 없이 경로 칸만
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareScheduleController.class, CareItemController.class,
        DailyHealthRecordController.class, CheckupController.class, CheckupAttachmentController.class,
        PrescriptionController.class, InspectionValueController.class, EtcInspectionController.class,
        RepeatPreventionController.class, HealthIndicatorController.class, ObservationController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class HealthCreateAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    CareItemService careItemService;
    @MockitoBean
    CareScheduleService careScheduleService;
    @MockitoBean
    CheckupAttachmentService checkupAttachmentService;
    @MockitoBean
    CheckupService checkupService;
    @MockitoBean
    DailyHealthRecordService dailyHealthRecordService;
    @MockitoBean
    EtcInspectionService etcInspectionService;
    @MockitoBean
    HealthIndicatorService healthIndicatorService;
    @MockitoBean
    InspectionValueService inspectionValueService;
    @MockitoBean
    ObservationService observationService;
    @MockitoBean
    PrescriptionService prescriptionService;
    @MockitoBean
    RepeatPreventionService repeatPreventionService;

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
    void 새_도구_6개가_건강_CARD로_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'health' && @.kind == 'CARD')].name",
                        hasItems("draft_checkup", "draft_prescription", "draft_etc_inspection",
                                "draft_repeat_prevention", "end_observation_item", "cancel_last_dose")));
    }

    @Test
    void draft_checkup_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("draft_checkup");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/checkups"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.ckupYmd.type").value("date"))
                .andExpect(jsonPath(t + ".params.ckupYmd.required").value(true))
                .andExpect(jsonPath(t + ".params.hsptlNm.required").value(true))
                .andExpect(jsonPath(t + ".params.hsptlNm.maxLength").value(80))
                .andExpect(jsonPath(t + ".params.ckupTypeCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(t + ".params.ckupTypeCd.required").value(true))
                .andExpect(jsonPath(t + ".params.ckupCn.maxLength").value(500))
                .andExpect(jsonPath(t + ".params.ckupMemoCn.maxLength").value(200))
                .andExpect(jsonPath(t + ".params.schdlSn").exists())
                .andExpect(jsonPath(t + ".params.*", hasSize(7)));
    }

    @Test
    void draft_prescription_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("draft_prescription");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/prescriptions"))
                .andExpect(jsonPath(t + ".params.ckupRcdSn.required").value(true))
                .andExpect(jsonPath(t + ".params.mdcnNm.maxLength").value(40))
                .andExpect(jsonPath(t + ".params.mdcnBgngYmd.type").value("date"))
                .andExpect(jsonPath(t + ".params.mdcnEndYmd.required").value(true))
                .andExpect(jsonPath(t + ".params.dayMdcnNmtm.min").value(1))
                .andExpect(jsonPath(t + ".params.dayMdcnNmtm.max").value(9999))
                .andExpect(jsonPath(t + ".params.prscSttsCd.group").value("HLTG06"))
                .andExpect(jsonPath(t + ".params.prscSttsCd.required").value(true))
                .andExpect(jsonPath(t + ".params.careRfltYn.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.*", hasSize(8)));
    }

    @Test
    void draft_etc_inspection_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("draft_etc_inspection");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/etc-inspections"))
                .andExpect(jsonPath(t + ".params.ckupRcdSn.required").value(true))
                .andExpect(jsonPath(t + ".params.inspNm.maxLength").value(100))
                .andExpect(jsonPath(t + ".params.inspRsltCn.required").value(true))
                .andExpect(jsonPath(t + ".params.inspRsltCn.maxLength").value(500))
                .andExpect(jsonPath(t + ".params.memoCn.maxLength").value(200))
                .andExpect(jsonPath(t + ".params.*", hasSize(5)));
    }

    @Test
    void draft_repeat_prevention_코드와_직접입력_주기_일수_규칙() throws Exception {
        givenCodes();
        String t = tool("draft_repeat_prevention");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/repeat-preventions"))
                .andExpect(jsonPath(t + ".params.reptPrvntArtclCd.group").value("HLTG07"))
                .andExpect(jsonPath(t + ".params.reptPrvntArtclCd.required").value(true))
                .andExpect(jsonPath(t + ".params.reptCycleCd.group").value("HLTG15"))
                .andExpect(jsonPath(t + ".params.reptCycleCd.required").value(true))
                .andExpect(jsonPath(t + ".params.cycleDrctInptDayCnt.min").value(1))
                .andExpect(jsonPath(t + ".params.cycleDrctInptDayCnt.max").value(365))
                .andExpect(jsonPath(t + ".params.bgngYmd.type").value("date"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.field").value("cycleDrctInptDayCnt"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.when").value("reptCycleCd"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.equals").value("HLTC0042"));
    }

    @Test
    void end_observation_item_은_본문_없이_경로_칸만() throws Exception {
        givenCodes();
        String t = tool("end_observation_item");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/observation-items/{obsrvArtclSn}/end"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.obsrvArtclSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }

    @Test
    void cancel_last_dose_는_DELETE이고_경로_칸만() throws Exception {
        givenCodes();
        String t = tool("cancel_last_dose");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".call.path")
                        .value("/api/pets/{petSn}/care-items/{careArtclSn}/today-completion/last"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.careArtclSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }
}
