package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.nyangmeong.health.dto.EtcInspectionResponse;
import com.nyangmeong.health.service.EtcInspectionService;

/**
 * [검사 - 기타검사 API]
 * - 결과 500자·메모 200자 제한 (FN-HLT-005-12·13)
 */
@WebMvcTest(controllers = EtcInspectionController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class EtcInspectionControllerTest {

    private static final String XHR = "X-Requested-With";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EtcInspectionService etcInspectionService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private String body(String inspRsltCn, String memoCn) {
        String memo = memoCn == null ? "" : ",\"memoCn\":\"" + memoCn + "\"";
        return "{\"ckupRcdSn\":500,\"inspNm\":\"체온\",\"inspRsltCn\":\"" + inspRsltCn + "\"" + memo + "}";
    }

    @Test
    @WithMockLoginMember
    void 메모_없이_입력하면_201() throws Exception {
        when(etcInspectionService.create(eq(1L), eq(10L), any()))
                .thenReturn(new EtcInspectionResponse(1L, 500L, "체온", "38.4℃", null));

        mockMvc.perform(post("/api/pets/10/etc-inspections").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("38.4℃", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inspNm").value("체온"));
    }

    @Test
    @WithMockLoginMember
    void 결과가_500자면_저장한다() throws Exception {
        when(etcInspectionService.create(eq(1L), eq(10L), any()))
                .thenReturn(new EtcInspectionResponse(1L, 500L, "체온", "가".repeat(500), null));

        mockMvc.perform(post("/api/pets/10/etc-inspections").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("가".repeat(500), null)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockLoginMember
    void 결과가_501자면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/etc-inspections").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("가".repeat(501), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(etcInspectionService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 메모가_201자면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/etc-inspections").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("38.4℃", "가".repeat(201))))
                .andExpect(status().isBadRequest());
        verify(etcInspectionService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 결과가_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/etc-inspections").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body(" ", null)))
                .andExpect(status().isBadRequest());
        verify(etcInspectionService, never()).create(anyLong(), anyLong(), any());
    }
}
