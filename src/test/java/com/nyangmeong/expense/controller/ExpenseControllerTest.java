package com.nyangmeong.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.dto.ExpenseCreateRequest;
import com.nyangmeong.expense.dto.ExpenseResponse;
import com.nyangmeong.expense.dto.ExpenseSummary;
import com.nyangmeong.expense.service.ExpenseService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
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
 * [검사 - 가계부 지출 등록·중복 확인 API]
 */
@WebMvcTest(controllers = ExpenseController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ExpenseControllerTest {

    private static final String BODY = """
            {"expndYmd":"2026-10-28","expndClsfCd":"MYPC0012","expndPlcusNm":"행복동물병원",
             "recptSn":3,"expndInptMthCd":"MYPC0020",
             "lines":[{"petSn":10,"expndAmt":447000,"idmptDmndId":"a"},
                      {"petSn":11,"expndAmt":15000,"idmptDmndId":"b"}]}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExpenseService expenseService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private List<ExpenseResponse> responses() {
        LocalDate ymd = LocalDate.of(2026, 10, 28);
        return List.of(new ExpenseResponse(100L, 10L, "아린", "MYPC0012", 447_000L, null, "행복동물병원", ymd, 3L),
                new ExpenseResponse(101L, 11L, "레오", "MYPC0012", 15_000L, null, "행복동물병원", ymd, 3L));
    }

    @Test
    void 쿠키가_없으면_401() throws Exception {
        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 등록은_201과_줄별_지출을_돌려준다() throws Exception {
        when(expenseService.create(eq(1L), any())).thenReturn(new ExpenseService.Created(responses(), true));

        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data[0].expndSn").value(100))
                .andExpect(jsonPath("$.data[1].petNm").value("레오"))
                .andExpect(jsonPath("$.data[1].recptSn").value(3));
    }

    @Test
    @WithMockLoginMember
    void 같은_요청을_다시_보내면_200으로_같은_결과다() throws Exception {
        when(expenseService.create(eq(1L), any())).thenReturn(new ExpenseService.Created(responses(), false));

        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].expndSn").value(100));
    }

    @Test
    @WithMockLoginMember
    void 금액이_0원이면_400이다() throws Exception {
        String body = BODY.replace("\"expndAmt\":15000", "\"expndAmt\":0");

        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(expenseService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember
    void 줄이_없거나_사용처가_80자를_넘으면_400이다() throws Exception {
        String noLines = BODY.substring(0, BODY.indexOf("\"lines\"")) + "\"lines\":[]}";
        String longPlace = BODY.replace("행복동물병원", "가".repeat(81));

        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(noLines)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(longPlace)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        verify(expenseService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember
    void 병원비를_공용으로_보내면_400_EXPENSE_PET_REQUIRED() throws Exception {
        when(expenseService.create(eq(1L), any())).thenThrow(new CustomException(ErrorCode.EXPENSE_PET_REQUIRED));

        mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content(BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPENSE_PET_REQUIRED"));
    }

    @Test
    @WithMockLoginMember
    void 중복_확인은_200과_겹치는_지출을_돌려준다() throws Exception {
        ExpenseSummary row = new ExpenseSummary();
        row.setExpndSn(7L);
        row.setPetSn(10L);
        row.setPetNm("아린");
        row.setExpndAmt(447_000L);
        when(expenseService.searchDuplicates(eq(1L), any(ExpenseCreateRequest.class)))
                .thenReturn(List.of(row));

        mockMvc.perform(post("/api/expenses/duplicate-check").contentType(MediaType.APPLICATION_JSON).content(BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].expndSn").value(7))
                .andExpect(jsonPath("$.data[0].petNm").value("아린"));
    }
}
