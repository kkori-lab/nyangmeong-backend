package com.nyangmeong.health.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.nyangmeong.health.dto.InspectionValueResponse;
import com.nyangmeong.health.dto.InspectionValueUpdateRequest;
import com.nyangmeong.health.service.InspectionValueService;

/**
 * [검사 - 검사 수치 수정 API]
 * - 참고범위는 refRange 묶음 : 안 보내면 유지, {null, null}이면 비움, 한쪽만·하한 > 상한이면 400 (FN-HLT-007-02·03)
 */
@WebMvcTest(controllers = InspectionValueController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class InspectionValueControllerTest {

    private static final String XHR = "X-Requested-With";
    private static final String URL = "/api/pets/10/inspection-values/1";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InspectionValueService inspectionValueService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private InspectionValueResponse response(BigDecimal llmt, BigDecimal uplmt) {
        return new InspectionValueResponse(1L, 500L, "HLTC0036", null, new BigDecimal("12.4"), "mg/dL", llmt, uplmt,
                null, 1);
    }

    @Test
    @WithMockLoginMember
    void 참고범위를_안_보내면_refRange는_null로_넘긴다() throws Exception {
        when(inspectionValueService.update(eq(1L), eq(10L), eq(1L), any()))
                .thenReturn(response(new BigDecimal("10"), new BigDecimal("100")));

        mockMvc.perform(patch(URL).header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"memoCn\":\"x\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<InspectionValueUpdateRequest> captor = ArgumentCaptor.forClass(InspectionValueUpdateRequest.class);
        verify(inspectionValueService).update(eq(1L), eq(10L), eq(1L), captor.capture());
        assertThat(captor.getValue().refRange()).isNull();
        assertThat(captor.getValue().memoCn()).isEqualTo("x");
    }

    @Test
    @WithMockLoginMember
    void 참고범위를_둘_다_null로_보내면_빈_묶음을_넘기고_200() throws Exception {
        when(inspectionValueService.update(eq(1L), eq(10L), eq(1L), any())).thenReturn(response(null, null));

        mockMvc.perform(patch(URL).header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refRange\":{\"refLlmtNvl\":null,\"refUplmtNvl\":null}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refLlmtNvl").doesNotExist())
                .andExpect(jsonPath("$.data.refUplmtNvl").doesNotExist());

        ArgumentCaptor<InspectionValueUpdateRequest> captor = ArgumentCaptor.forClass(InspectionValueUpdateRequest.class);
        verify(inspectionValueService).update(eq(1L), eq(10L), eq(1L), captor.capture());
        assertThat(captor.getValue().refRange()).isNotNull();
        assertThat(captor.getValue().refRange().refLlmtNvl()).isNull();
        assertThat(captor.getValue().refRange().refUplmtNvl()).isNull();
    }

    @Test
    @WithMockLoginMember
    void 참고범위가_한쪽만_있으면_400() throws Exception {
        mockMvc.perform(patch(URL).header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refRange\":{\"refLlmtNvl\":null,\"refUplmtNvl\":10}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("refRange.paired"));
        verify(inspectionValueService, never()).update(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 참고범위_하한이_상한보다_크면_400() throws Exception {
        mockMvc.perform(patch(URL).header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refRange\":{\"refLlmtNvl\":50,\"refUplmtNvl\":10}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("refRange.ordered"));
        verify(inspectionValueService, never()).update(anyLong(), anyLong(), anyLong(), any());
    }
}
