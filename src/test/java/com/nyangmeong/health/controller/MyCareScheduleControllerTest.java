package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import com.nyangmeong.health.dto.CareScheduleSummary;
import com.nyangmeong.health.service.CareScheduleService;

/**
 * [검사 - 회원 단위 케어 캘린더 일정 API]
 */
@WebMvcTest(controllers = MyCareScheduleController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class MyCareScheduleControllerTest {

    private static final String PATH = "/api/care-schedules";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CareScheduleService careScheduleService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private static String joined(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }

    @Test
    @WithMockLoginMember
    void 반려동물_번호와_검색_조건을_받아_로그인_회원으로_조회한다() throws Exception {
        CareScheduleSummary row = new CareScheduleSummary();
        row.setSchdlSn(100L);
        row.setPetSn(10L);
        when(careScheduleService.searchMine(eq(1L), any())).thenReturn(List.of(row));

        mockMvc.perform(get(PATH).param("petSns", "10,11").param("fromYmd", "2026-11-01")
                .param("toYmd", "2026-11-30").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].schdlSn").value(100))
                .andExpect(jsonPath("$.data[0].petSn").value(10));

        verify(careScheduleService).searchMine(eq(1L), argThat(cond -> "10,11".equals(cond.getPetSns())
                && LocalDate.of(2026, 11, 1).equals(cond.getFromYmd())
                && LocalDate.of(2026, 11, 30).equals(cond.getToYmd())
                && Integer.valueOf(3).equals(cond.getSize())));
    }

    @Test
    @WithMockLoginMember
    void petSns를_생략해도_200() throws Exception {
        when(careScheduleService.searchMine(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(get(PATH)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());

        verify(careScheduleService).searchMine(eq(1L), argThat(cond -> cond.getPetSns() == null));
    }

    @Test
    @WithMockLoginMember
    void petSns가_빈_문자열이어도_200() throws Exception {
        when(careScheduleService.searchMine(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(get(PATH).param("petSns", "")).andExpect(status().isOk());

        verify(careScheduleService).searchMine(eq(1L), any());
    }

    @Test
    @WithMockLoginMember
    void petSns_형식이_틀리면_400() throws Exception {
        for (String petSns : List.of("1,,2", "a", "1,", ",1", "1 2")) {
            mockMvc.perform(get(PATH).param("petSns", petSns))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        }
        verify(careScheduleService, never()).searchMine(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 반려동물_번호가_51개_이상이면_400() throws Exception {
        when(careScheduleService.searchMine(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get(PATH).param("petSns", joined(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void size가_50을_넘으면_400() throws Exception {
        mockMvc.perform(get(PATH).param("size", "51")).andExpect(status().isBadRequest());
        verify(careScheduleService, never()).searchMine(anyLong(), any());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
}
