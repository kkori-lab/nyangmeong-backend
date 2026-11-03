package com.nyangmeong.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.dto.ExpenseLimitResponse;
import com.nyangmeong.expense.service.ExpenseLimitService;
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
 * [검사 - 월 지출 한도 저장 API]
 */
@WebMvcTest(controllers = ExpenseLimitController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ExpenseLimitControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExpenseLimitService expenseLimitService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 한도를_저장하면_알림_수신_여부와_함께_돌려준다() throws Exception {
        when(expenseLimitService.save(eq(1L), any())).thenReturn(new ExpenseLimitResponse(500_000L, true));

        mockMvc.perform(put("/api/members/me/expense-limit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mmExpndLimAmt\":500000}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mmExpndLimAmt").value(500000))
                .andExpect(jsonPath("$.data.expndLimNotiRcptnYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 한도가_없거나_0원이면_400이다() throws Exception {
        mockMvc.perform(put("/api/members/me/expense-limit").contentType(MediaType.APPLICATION_JSON)
                .content("{}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/members/me/expense-limit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mmExpndLimAmt\":0}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        verify(expenseLimitService, never()).save(any(), any());
    }
}
