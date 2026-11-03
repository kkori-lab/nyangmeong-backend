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

import com.nyangmeong.ai.dto.RegisterContextPet;
import com.nyangmeong.ai.service.AiRegisterContextService;
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
 * [검사 - /internal/ai/register-context API]
 */
@WebMvcTest(controllers = AiRegisterContextController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AiRegisterContextControllerTest {

    private static final String PATH = "/internal/ai/register-context";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiRegisterContextService aiRegisterContextService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 반려동물과_케어_항목_이름을_배열로_준다() throws Exception {
        when(aiRegisterContextService.search(1L))
                .thenReturn(List.of(new RegisterContextPet(10L, "콩이", List.of("심장사상충"))));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].petSn").value(10))
                .andExpect(jsonPath("$.data[0].petNm").value("콩이"))
                .andExpect(jsonPath("$.data[0].careArtclNms[0]").value("심장사상충"));
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 반려동물이_없으면_빈_배열이다() throws Exception {
        when(aiRegisterContextService.search(1L)).thenReturn(List.of());

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
