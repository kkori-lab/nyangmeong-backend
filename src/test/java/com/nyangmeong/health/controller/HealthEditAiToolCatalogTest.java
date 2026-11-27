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
import org.springframework.http.MediaType;
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
 * [검사 - 건강 도메인 AI 수정·삭제 도구]
 * - 수정 6개는 PATCH 본문 칸·코드와 함께, 삭제 9개는 DELETE에 경로 칸만 실리는지
 * - 도구 목록이 싣지 못하는 칸(@AiExclude)은 그 칸만 빠지고 도구는 실린다
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareScheduleController.class, CareItemController.class,
        DailyHealthRecordController.class, CheckupController.class, CheckupAttachmentController.class,
        PrescriptionController.class, InspectionValueController.class, EtcInspectionController.class,
        RepeatPreventionController.class, HealthIndicatorController.class, ObservationController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class HealthEditAiToolCatalogTest {

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
    void 일지_수정_도구는_PATCH이고_배변_상태_코드를_준다() throws Exception {
        givenCodes();
        String t = tool("draft_daily_health_record_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("health"))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/daily-health-records/{dailyHlthRcdSn}"))
                .andExpect(jsonPath(t + ".params.dailyHlthRcdSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.rcdDt.type").value("datetime"))
                .andExpect(jsonPath(t + ".params.rcdNvl.in").value("body"))
                .andExpect(jsonPath(t + ".params.crsdSttsCd.group").value("HLTG10"))
                .andExpect(jsonPath(t + ".params.crsdSttsCd.required").doesNotExist());
    }

    @Test
    void 검진_처방_기타검사_수정_도구는_PATCH이고_보낸_칸만_고친다() throws Exception {
        givenCodes();
        String c = tool("draft_checkup_update");
        String p = tool("draft_prescription_update");
        String e = tool("draft_etc_inspection_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(c + ".call.method").value("PATCH"))
                .andExpect(jsonPath(c + ".call.path").value("/api/pets/{petSn}/checkups/{ckupRcdSn}"))
                .andExpect(jsonPath(c + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(c + ".params.hsptlNm.maxLength").value(80))
                .andExpect(jsonPath(c + ".params.hsptlNm.required").doesNotExist())
                .andExpect(jsonPath(p + ".call.method").value("PATCH"))
                .andExpect(jsonPath(p + ".call.path").value("/api/pets/{petSn}/prescriptions/{prscSn}"))
                .andExpect(jsonPath(p + ".params.prscSttsCd.group").value("HLTG06"))
                .andExpect(jsonPath(p + ".params.mdcnEndYmd.type").value("date"))
                .andExpect(jsonPath(p + ".params.careRfltYn.type").value("boolean"))
                .andExpect(jsonPath(e + ".call.method").value("PATCH"))
                .andExpect(jsonPath(e + ".call.path").value("/api/pets/{petSn}/etc-inspections/{etcInspSn}"))
                .andExpect(jsonPath(e + ".params.inspNm.maxLength").value(100))
                .andExpect(jsonPath(e + ".params.inspRsltCn.maxLength").value(500));
    }

    @Test
    void 검사_수치_수정_도구는_참고범위_칸만_빼고_싣는다() throws Exception {
        givenCodes();
        String t = tool("draft_inspection_value_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/inspection-values/{inspNvlSn}"))
                .andExpect(jsonPath(t + ".params.rsltNvl.type").value("decimal"))
                .andExpect(jsonPath(t + ".params.unitNm.maxLength").value(20))
                .andExpect(jsonPath(t + ".params.memoCn.maxLength").value(200))
                .andExpect(jsonPath(t + ".params.refRange").doesNotExist());
    }

    @Test
    void 관찰_회차_수정_도구는_multipart이고_비우기_칸만_빼고_싣는다() throws Exception {
        givenCodes();
        String t = tool("draft_observation_cycle_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/observation-cycles/{obsrvCyclSn}"))
                .andExpect(jsonPath(t + ".call.contentType").value(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(jsonPath(t + ".params.file.type").value("file"))
                .andExpect(jsonPath(t + ".params.file.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.obsrvChgCd.group").value("HLTG17"))
                .andExpect(jsonPath(t + ".params.obsrvSz.type").value("decimal"))
                .andExpect(jsonPath(t + ".params.clearFields").doesNotExist());
    }

    @Test
    void 삭제_도구_9개는_DELETE이고_경로_칸만_있다() throws Exception {
        givenCodes();
        var result = mockMvc.perform(get(PATH));
        String[][] deletes = {
                {"delete_schedule", "/api/pets/{petSn}/care-schedules/{schdlSn}", "schdlSn"},
                {"delete_care_item", "/api/pets/{petSn}/care-items/{careArtclSn}", "careArtclSn"},
                {"cancel_care_completion", "/api/pets/{petSn}/care-completions/{careCmptnHstrySn}",
                        "careCmptnHstrySn"},
                {"delete_daily_health_record", "/api/pets/{petSn}/daily-health-records/{dailyHlthRcdSn}",
                        "dailyHlthRcdSn"},
                {"delete_checkup", "/api/pets/{petSn}/checkups/{ckupRcdSn}", "ckupRcdSn"},
                {"delete_prescription", "/api/pets/{petSn}/prescriptions/{prscSn}", "prscSn"},
                {"delete_etc_inspection", "/api/pets/{petSn}/etc-inspections/{etcInspSn}", "etcInspSn"},
                {"delete_observation_cycle", "/api/pets/{petSn}/observation-cycles/{obsrvCyclSn}", "obsrvCyclSn"},
                {"delete_repeat_prevention", "/api/pets/{petSn}/repeat-preventions/{reptPrvntMngSn}",
                        "reptPrvntMngSn"}};
        for (String[] d : deletes) {
            String t = tool(d[0]);
            result.andExpect(jsonPath(t + ".kind").value("CARD"))
                    .andExpect(jsonPath(t + ".domain").value("health"))
                    .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                    .andExpect(jsonPath(t + ".call.path").value(d[1]))
                    .andExpect(jsonPath(t + ".call.contentType").doesNotExist())
                    .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                    .andExpect(jsonPath(t + ".params.petSn.required").value(true))
                    .andExpect(jsonPath(t + ".params." + d[2] + ".in").value("path"))
                    .andExpect(jsonPath(t + ".params." + d[2] + ".type").value("long"))
                    .andExpect(jsonPath(t + ".params.*", hasSize(2)));
        }
    }
}
