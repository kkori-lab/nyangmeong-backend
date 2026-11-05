package com.nyangmeong.health.controller;

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
import com.nyangmeong.health.service.CareItemService;
import com.nyangmeong.health.service.CareScheduleService;

/**
 * [검사 - 일정·케어 항목 등록의 AI 매니저 도구 표시]
 * - draft_schedule·draft_care_item 칸·필수·조건부 규칙·공통코드
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareScheduleController.class, CareItemController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class CareAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";
    private static final String SCHEDULE = "$.data.tools[?(@.name == 'draft_schedule')]";
    private static final String CARE_ITEM = "$.data.tools[?(@.name == 'draft_care_item')]";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CareScheduleService careScheduleService;

    @MockitoBean
    CareItemService careItemService;

    @MockitoBean
    CommonCodeService commonCodeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private void givenCodes() {
        CommonCodeSummary hospital = new CommonCodeSummary();
        hospital.setComCd("HLTC0057");
        hospital.setComCdNm("병원");
        when(commonCodeService.search(anyString())).thenReturn(List.of());
        when(commonCodeService.search("HLTG19")).thenReturn(List.of(hospital));
    }

    @Test
    void 일정_등록_도구를_준다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SCHEDULE + ".domain").value("health"))
                .andExpect(jsonPath(SCHEDULE + ".kind").value("CARD"))
                .andExpect(jsonPath(SCHEDULE + ".call.method").value("POST"))
                .andExpect(jsonPath(SCHEDULE + ".call.path").value("/api/pets/{petSn}/care-schedules"))
                .andExpect(jsonPath(SCHEDULE + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(SCHEDULE + ".params.petSn.required").value(true))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlKndCd.type").value("code"))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlKndCd.group").value("HLTG19"))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlKndCd.required").value(true))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlKndCd.codes[0].cd").value("HLTC0057"))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlKndCd.codes[0].nm").value("병원"))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlDt.type").value("datetime"))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlDt.required").value(true))
                .andExpect(jsonPath(SCHEDULE + ".params.hrInptYn.type").value("boolean"))
                .andExpect(jsonPath(SCHEDULE + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(SCHEDULE + ".params.hsptlNm.maxLength").value(80))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlNm.maxLength").value(40))
                .andExpect(jsonPath(SCHEDULE + ".params.memoCn.maxLength").value(500))
                .andExpect(jsonPath(SCHEDULE + ".params.cldrBadgeColrCd.group").value("HLTG21"))
                .andExpect(jsonPath(SCHEDULE + ".params.schdlSrcCd.group").value("HLTG22"))
                .andExpect(jsonPath(SCHEDULE + ".rules.length()").value(5))
                .andExpect(jsonPath(SCHEDULE + ".rules[0].requiredIf.field").value("ckupTypeCd"))
                .andExpect(jsonPath(SCHEDULE + ".rules[0].requiredIf.equals").value("HLTC0057"))
                .andExpect(jsonPath(SCHEDULE + ".rules[1].requiredIf.field").value("hsptlNm"))
                .andExpect(jsonPath(SCHEDULE + ".rules[4].requiredIf.field").value("schdlNm"))
                .andExpect(jsonPath(SCHEDULE + ".rules[4].requiredIf.equals").value("HLTC0060"));
    }

    @Test
    void 케어_항목_추가_도구를_준다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CARE_ITEM + ".kind").value("CARD"))
                .andExpect(jsonPath(CARE_ITEM + ".call.method").value("POST"))
                .andExpect(jsonPath(CARE_ITEM + ".call.path").value("/api/pets/{petSn}/care-items"))
                .andExpect(jsonPath(CARE_ITEM + ".params.careArtclNm.required").value(true))
                .andExpect(jsonPath(CARE_ITEM + ".params.careArtclNm.maxLength").value(40))
                .andExpect(jsonPath(CARE_ITEM + ".params.careZoneCd.group").value("HLTG13"))
                .andExpect(jsonPath(CARE_ITEM + ".params.careZoneCd.required").value(true))
                .andExpect(jsonPath(CARE_ITEM + ".params.careCycleCd.group").value("HLTG15"))
                .andExpect(jsonPath(CARE_ITEM + ".params.cycleDrctInptDayCnt.type").value("int"))
                .andExpect(jsonPath(CARE_ITEM + ".params.cycleDrctInptDayCnt.min").value(1))
                .andExpect(jsonPath(CARE_ITEM + ".params.cycleDrctInptDayCnt.max").value(9999))
                .andExpect(jsonPath(CARE_ITEM + ".params.lastCmptnYmd.type").value("date"))
                .andExpect(jsonPath(CARE_ITEM + ".rules[0].requiredIf.field").value("careCycleCd"))
                .andExpect(jsonPath(CARE_ITEM + ".rules[0].requiredIf.equals").value("HLTC0037"))
                .andExpect(jsonPath(CARE_ITEM + ".rules[1].requiredIf.field").value("cycleDrctInptDayCnt"))
                .andExpect(jsonPath(CARE_ITEM + ".rules[1].requiredIf.when").value("careCycleCd"))
                .andExpect(jsonPath(CARE_ITEM + ".rules[1].requiredIf.equals").value("HLTC0042"));
    }
}
