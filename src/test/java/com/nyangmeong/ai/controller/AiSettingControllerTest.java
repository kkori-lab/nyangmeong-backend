package com.nyangmeong.ai.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.ai.dto.AiSettingResponse;
import com.nyangmeong.ai.service.AiSettingService;
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
 * [검사 - /internal/ai/settings API]
 */
@WebMvcTest(controllers = AiSettingController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AiSettingControllerTest {

    private static final String PATH = "/internal/ai/settings";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiSettingService aiSettingService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 토큰_상한과_대화_턴_수를_준다() throws Exception {
        when(aiSettingService.getSetting()).thenReturn(new AiSettingResponse(4500, 6));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiTknUplmtCnt").value(4500))
                .andExpect(jsonPath("$.data.aiCnvrsTurnCnt").value(6));
    }
}
