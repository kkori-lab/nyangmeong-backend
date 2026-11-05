package com.nyangmeong.global.external;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

/**
 * [검사 - 외부 API 사용량·호출 통지·검사결과지 분석 현황 API]
 */
@WebMvcTest(controllers = {AdminExternalApiUsageController.class, ExternalApiCallInternalController.class,
        AdminPdfAnalysisController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ExternalApiControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String USER = "USRC0001";
    private static final String REQUESTED_WITH = "X-Requested-With";
    private static final String NOTIFY_PATH = "/internal/ai/external-api-calls";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExternalApiUsageService externalApiUsageService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_사용량을_조회한다() throws Exception {
        ExternalApiUsageSummary row = ExternalApiUsageSummary.empty(ExternalApiType.OCR);
        row.setCallCnt(4);
        row.setFailCnt(1);
        when(externalApiUsageService.search(any())).thenReturn(List.of(row));

        mockMvc.perform(get("/api/admin/external-api-usage").param("bgngYmd", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].extlApiNm").value("OCR"))
                .andExpect(jsonPath("$.data[0].failRate").value(25.0));
    }

    @Test
    @WithMockLoginMember(id = 5L, mbrAuthrtCd = USER)
    void 일반_회원은_사용량을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/external-api-usage/tokens"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(externalApiUsageService);
    }

    @Test
    void 비로그인은_실패_목록을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/external-api-usage/failures"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 5L, mbrAuthrtCd = USER)
    void 통지하면_201로_로그_번호를_돌려준다() throws Exception {
        when(externalApiUsageService.record(eq(5L), any())).thenReturn(new ExternalApiCallResponse(7L));

        mockMvc.perform(post(NOTIFY_PATH).header(REQUESTED_WITH, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"extlApiSeCd\":\"SYSC0015\",\"callRsltCd\":\"SYSC0021\",\"inptTknCnt\":120}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.extlApiCallLogSn").value(7));
    }

    @Test
    @WithMockLoginMember(id = 5L, mbrAuthrtCd = USER)
    void 통지_필수값이_없으면_400() throws Exception {
        mockMvc.perform(post(NOTIFY_PATH).header(REQUESTED_WITH, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"extlApiSeCd\":\"SYSC0015\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(externalApiUsageService);
    }

    @Test
    void 비로그인은_통지할_수_없다() throws Exception {
        mockMvc.perform(post(NOTIFY_PATH).header(REQUESTED_WITH, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"extlApiSeCd\":\"SYSC0015\",\"callRsltCd\":\"SYSC0021\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(externalApiUsageService);
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_검사결과지_분석_현황을_조회한다() throws Exception {
        PdfAnalysisSummary summary = new PdfAnalysisSummary();
        summary.setTotCnt(3);
        summary.setTmotCnt(1);
        PdfAnalysisFailureRow row = new PdfAnalysisFailureRow();
        row.setExtlApiCallLogSn(11L);
        row.setCallRsltCd("SYSC0023");
        when(externalApiUsageService.searchPdfAnalyses(any(), any()))
                .thenReturn(new PdfAnalysisListResponse(summary, List.of(row), 1, 20, 1, 1, false));

        mockMvc.perform(get("/api/admin/processing/pdf-analyses").param("fromYmd", "2026-11-01")
                .param("toYmd", "2026-11-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.totCnt").value(3))
                .andExpect(jsonPath("$.data.summary.tmotCnt").value(1))
                .andExpect(jsonPath("$.data.content[0].extlApiCallLogSn").value(11))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockLoginMember(id = 5L, mbrAuthrtCd = USER)
    void 일반_회원은_검사결과지_분석_현황을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/processing/pdf-analyses"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(externalApiUsageService);
    }
}
