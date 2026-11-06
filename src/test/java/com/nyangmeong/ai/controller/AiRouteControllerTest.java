package com.nyangmeong.ai.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.ai.dto.AiRouteResponse;
import com.nyangmeong.ai.service.AiRouteService;
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
 * [검사 - AI 서버용 라우팅 매핑표 내부 API]
 */
@WebMvcTest(controllers = AiRouteController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AiRouteControllerTest {

    private static final String PATH = "/internal/ai/routes";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiRouteService aiRouteService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 사용_중_매핑을_지시어_배열로_준다() throws Exception {
        when(aiRouteService.getActiveRoutes()).thenReturn(List.of(new AiRouteResponse(2L, "플레이스 병원",
                "SCR-HOS-001", "cat=hospital", List.of("병원", "동물병원"), "AIMC0032", true, 2)));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scrnId").value("SCR-HOS-001"))
                .andExpect(jsonPath("$.data[0].instKwrds[1]").value("동물병원"))
                .andExpect(jsonPath("$.data[0].rprsChipYn").value(true));
    }
}
