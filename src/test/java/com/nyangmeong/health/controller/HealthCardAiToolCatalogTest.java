package com.nyangmeong.health.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.nyangmeong.health.service.DailyHealthRecordService;
import com.nyangmeong.health.service.ObservationService;

/**
 * [검사 - 건강 도메인 AI 쓰기 도구]
 * - 일정 수정·케어 항목 수정·케어 완료·건강 일지·관찰 회차가 CARD로 칸·규칙과 함께 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareScheduleController.class, CareItemController.class,
        DailyHealthRecordController.class, ObservationController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class HealthCardAiToolCatalogTest {

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
    DailyHealthRecordService dailyHealthRecordService;
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
    void 건강_쓰기_CARD_7개가_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'CARD')].name", containsInAnyOrder("draft_care_completion",
                        "draft_care_item", "draft_care_item_update", "draft_daily_health_record",
                        "draft_observation_cycle", "draft_schedule", "draft_schedule_update")));
    }

    @Test
    void 일정_수정_도구는_PATCH이고_옮길_반려동물_칸은_뺀다() throws Exception {
        givenCodes();
        String t = tool("draft_schedule_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".domain").value("health"))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/care-schedules/{schdlSn}"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.schdlSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.schdlKndCd.group").value("HLTG19"))
                .andExpect(jsonPath(t + ".params.schdlKndCd.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(t + ".params.cldrBadgeColrCd.group").value("HLTG21"))
                .andExpect(jsonPath(t + ".params.schdlNm.maxLength").value(40))
                .andExpect(jsonPath(t + ".params.length()").value(10));
    }

    @Test
    void 케어_항목_수정_도구는_직접입력_주기의_일수_규칙을_준다() throws Exception {
        givenCodes();
        String t = tool("draft_care_item_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/care-items/{careArtclSn}"))
                .andExpect(jsonPath(t + ".params.careZoneCd.group").value("HLTG13"))
                .andExpect(jsonPath(t + ".params.careCycleCd.group").value("HLTG15"))
                .andExpect(jsonPath(t + ".params.cycleDrctInptDayCnt.max").value(9999))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.field").value("cycleDrctInptDayCnt"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.when").value("careCycleCd"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.equals").value("HLTC0042"));
    }

    @Test
    void 케어_완료_체크_도구는_항목_번호가_필수다() throws Exception {
        givenCodes();
        String t = tool("draft_care_completion");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/care-completions"))
                .andExpect(jsonPath(t + ".params.careArtclSn.type").value("long"))
                .andExpect(jsonPath(t + ".params.careArtclSn.required").value(true));
    }

    @Test
    void 건강_일지_도구는_지표별_필수_규칙과_코드를_준다() throws Exception {
        givenCodes();
        String t = tool("draft_daily_health_record");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/daily-health-records"))
                .andExpect(jsonPath(t + ".params.dailyRcdIdctCd.group").value("HLTG09"))
                .andExpect(jsonPath(t + ".params.dailyRcdIdctCd.required").value(true))
                .andExpect(jsonPath(t + ".params.crsdSttsCd.group").value("HLTG10"))
                .andExpect(jsonPath(t + ".params.rcdDt.type").value("datetime"))
                .andExpect(jsonPath(t + ".params.rcdNvl.scale").value(3))
                .andExpect(jsonPath(t + ".rules.length()").value(5))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.field").value("crsdSttsCd"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.equals").value("HLTC0017"))
                .andExpect(jsonPath(t + ".rules[1].requiredIf.field").value("rcdNvl"))
                .andExpect(jsonPath(t + ".rules[4].requiredIf.equals").value("HLTC0018"));
    }

    @Test
    void 관찰_회차_도구는_multipart이고_사진_칸이_필수다() throws Exception {
        givenCodes();
        String t = tool("draft_observation_cycle");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/observation-cycles"))
                .andExpect(jsonPath(t + ".call.contentType").value("multipart/form-data"))
                .andExpect(jsonPath(t + ".params.file.in").value("body"))
                .andExpect(jsonPath(t + ".params.file.type").value("file"))
                .andExpect(jsonPath(t + ".params.file.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvArtclSn.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvDt.required").value(true))
                .andExpect(jsonPath(t + ".params.obsrvChgCd.group").value("HLTG17"))
                .andExpect(jsonPath(t + ".params.memoCn.maxLength").value(200));
    }

    @Test
    void 기존_카드_도구는_multipart가_아니다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("draft_schedule") + ".call.contentType").isEmpty())
                .andExpect(jsonPath(tool("draft_care_item") + ".call.contentType").isEmpty());
    }
}
