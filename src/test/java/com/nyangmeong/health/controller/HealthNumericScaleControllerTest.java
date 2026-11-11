package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.health.service.DailyHealthRecordService;
import com.nyangmeong.health.service.InspectionValueService;
import com.nyangmeong.health.service.ObservationService;

/**
 * [건강기록 수치 소수 자리 검증]
 * - 일상 기록 수치·관찰 크기·검사 수치는 소수 둘째 자리까지, 셋째 자리부터 400
 */
@WebMvcTest(controllers = {DailyHealthRecordController.class, ObservationController.class,
        InspectionValueController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class HealthNumericScaleControllerTest {

    private static final String XHR = "X-Requested-With";
    private static final String DAILY_URL = "/api/pets/10/daily-health-records";
    private static final String CYCLE_URL = "/api/pets/10/observation-cycles";
    private static final String ITEM_URL = "/api/pets/10/observation-items";
    private static final String INSP_URL = "/api/pets/10/inspection-values";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DailyHealthRecordService dailyHealthRecordService;

    @MockitoBean
    ObservationService observationService;

    @MockitoBean
    InspectionValueService inspectionValueService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[] {1});
    }

    private String dailyCreate(String nvl) {
        return "{\"dailyRcdIdctCd\":\"HLTC0001\",\"rcdDt\":\"2026-11-01T10:00:00\",\"rcdNvl\":" + nvl + "}";
    }

    private String inspCreate(String rslt, String llmt, String uplmt) {
        return "{\"ckupRcdSn\":500,\"values\":[{\"inspArtclCd\":\"HLTC0036\",\"rsltNvl\":" + rslt
                + ",\"unitNm\":\"mg/dL\",\"refLlmtNvl\":" + llmt + ",\"refUplmtNvl\":" + uplmt + "}]}";
    }

    private String inspUpdateRange(String llmt, String uplmt) {
        return "{\"refRange\":{\"refLlmtNvl\":" + llmt + ",\"refUplmtNvl\":" + uplmt + "}}";
    }

    // 일상 건강기록

    @Test
    @WithMockLoginMember
    void 일상기록_등록_소수_둘째_자리는_201() throws Exception {
        mockMvc.perform(json(post(DAILY_URL), dailyCreate("4.23"))).andExpect(status().isCreated());
        verify(dailyHealthRecordService).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 일상기록_등록_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(json(post(DAILY_URL), dailyCreate("4.231")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(dailyHealthRecordService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 일상기록_수정_소수_둘째_자리는_200() throws Exception {
        mockMvc.perform(json(patch(DAILY_URL + "/1"), "{\"rcdNvl\":4.23}")).andExpect(status().isOk());
        verify(dailyHealthRecordService).update(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 일상기록_수정_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(json(patch(DAILY_URL + "/1"), "{\"rcdNvl\":4.231}"))
                .andExpect(status().isBadRequest());
        verify(dailyHealthRecordService, never()).update(anyLong(), anyLong(), anyLong(), any());
    }

    // 관찰 회차·항목

    @Test
    @WithMockLoginMember
    void 관찰회차_추가_소수_둘째_자리는_201() throws Exception {
        mockMvc.perform(multipart(CYCLE_URL).file(photo()).header(XHR, "XMLHttpRequest")
                .param("obsrvArtclSn", "1").param("obsrvDt", "2026-11-01T10:00:00").param("obsrvSz", "1.25"))
                .andExpect(status().isCreated());
        verify(observationService).createCycle(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 관찰회차_추가_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(multipart(CYCLE_URL).file(photo()).header(XHR, "XMLHttpRequest")
                .param("obsrvArtclSn", "1").param("obsrvDt", "2026-11-01T10:00:00").param("obsrvSz", "1.255"))
                .andExpect(status().isBadRequest());
        verify(observationService, never()).createCycle(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 관찰회차_수정_소수_둘째_자리는_200() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, CYCLE_URL + "/1").header(XHR, "XMLHttpRequest")
                .param("obsrvSz", "1.25"))
                .andExpect(status().isOk());
        verify(observationService).updateCycle(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 관찰회차_수정_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, CYCLE_URL + "/1").header(XHR, "XMLHttpRequest")
                .param("obsrvSz", "1.255"))
                .andExpect(status().isBadRequest());
        verify(observationService, never()).updateCycle(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 관찰항목_등록_소수_둘째_자리는_201() throws Exception {
        mockMvc.perform(multipart(ITEM_URL).file(photo()).header(XHR, "XMLHttpRequest")
                .param("obsrvArtclNm", "혹").param("obsrvRegnCd", "HLTC0050")
                .param("obsrvDt", "2026-11-01T10:00:00").param("obsrvSz", "1.25"))
                .andExpect(status().isCreated());
        verify(observationService).createItem(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 관찰항목_등록_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(multipart(ITEM_URL).file(photo()).header(XHR, "XMLHttpRequest")
                .param("obsrvArtclNm", "혹").param("obsrvRegnCd", "HLTC0050")
                .param("obsrvDt", "2026-11-01T10:00:00").param("obsrvSz", "1.255"))
                .andExpect(status().isBadRequest());
        verify(observationService, never()).createItem(anyLong(), anyLong(), any());
    }

    // 검사 수치

    @Test
    @WithMockLoginMember
    void 검사수치_저장_소수_둘째_자리는_201() throws Exception {
        mockMvc.perform(json(post(INSP_URL), inspCreate("1.25", "0.55", "1.75"))).andExpect(status().isCreated());
        verify(inspectionValueService).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 검사수치_저장_결과_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(json(post(INSP_URL), inspCreate("1.255", "null", "null")))
                .andExpect(status().isBadRequest());
        verify(inspectionValueService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 검사수치_저장_참고범위_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(json(post(INSP_URL), inspCreate("1.25", "0.555", "1.75")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post(INSP_URL), inspCreate("1.25", "0.55", "1.755")))
                .andExpect(status().isBadRequest());
        verify(inspectionValueService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 검사수치_수정_소수_둘째_자리는_200() throws Exception {
        mockMvc.perform(json(patch(INSP_URL + "/1"), "{\"rsltNvl\":1.25}")).andExpect(status().isOk());
        mockMvc.perform(json(patch(INSP_URL + "/1"), inspUpdateRange("0.55", "1.75"))).andExpect(status().isOk());
    }

    @Test
    @WithMockLoginMember
    void 검사수치_수정_소수_셋째_자리는_400() throws Exception {
        mockMvc.perform(json(patch(INSP_URL + "/1"), "{\"rsltNvl\":1.255}")).andExpect(status().isBadRequest());
        mockMvc.perform(json(patch(INSP_URL + "/1"), inspUpdateRange("0.555", "1.75")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(patch(INSP_URL + "/1"), inspUpdateRange("0.55", "1.755")))
                .andExpect(status().isBadRequest());
        verify(inspectionValueService, never()).update(anyLong(), anyLong(), anyLong(), any());
    }
}
