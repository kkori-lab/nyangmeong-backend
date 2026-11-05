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

import com.nyangmeong.ai.dto.AiKeywordResponse;
import com.nyangmeong.ai.service.AiKeywordService;
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
 * [검사 - AI 서버용 키워드 사전 내부 API]
 */
@WebMvcTest(controllers = AiKeywordController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AiKeywordControllerTest {

    private static final String PATH = "/internal/ai/keywords";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiKeywordService aiKeywordService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(get(PATH).param("dictUsgCd", "AIMC0037")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 사용_중_키워드를_예시_문구_그대로_준다() throws Exception {
        when(aiKeywordService.getActiveKeywords("AIMC0037")).thenReturn(List.of(new AiKeywordResponse(2L,
                "등록 동사", "일정·항목 등록", "잡아줘, 등록해줘", "등록 의도", null)));

        mockMvc.perform(get(PATH).param("dictUsgCd", "AIMC0037"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].kwrdDictSn").value(2))
                .andExpect(jsonPath("$.data[0].kwrdClsfNm").value("등록 동사"))
                .andExpect(jsonPath("$.data[0].kwrdCn").value("잡아줘, 등록해줘"));
    }
}
