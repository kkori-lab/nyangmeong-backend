package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.nyangmeong.health.service.InspectionValueService;

/**
 * [검사 - 검사 수치 저장 API의 요청 검증 칸 이름]
 * - AI 도구 draft_inspection_values : 잘못된 행은 400 data에 values[i].<칸>
 */
@WebMvcTest(controllers = InspectionValueController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class InspectionValueCreateControllerTest {

    private static final String XHR = "X-Requested-With";
    private static final String URL = "/api/pets/10/inspection-values";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InspectionValueService inspectionValueService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 결과값이_음수면_400_data에_values_0_rsltNvl() throws Exception {
        mockMvc.perform(post(URL).header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ckupRcdSn\":500,\"values\":[{\"inspArtclCd\":\"HLTC0036\",\"rsltNvl\":-1,"
                        + "\"unitNm\":\"mg/dL\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("values[0].rsltNvl"));
        verify(inspectionValueService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 둘째_행_단위가_없고_소수_셋째_자리면_두_칸_모두() throws Exception {
        mockMvc.perform(post(URL).header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ckupRcdSn\":500,\"values\":[{\"inspArtclCd\":\"HLTC0036\",\"rsltNvl\":1,"
                        + "\"unitNm\":\"u\"},{\"inspArtclDrctInptNm\":\"SDMA\",\"rsltNvl\":1.234}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'values[1].unitNm')]").exists())
                .andExpect(jsonPath("$.data[?(@.field == 'values[1].rsltNvl')]").exists());
    }
}
