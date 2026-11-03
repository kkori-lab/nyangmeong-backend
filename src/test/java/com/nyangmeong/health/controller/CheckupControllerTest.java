package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.service.CheckupService;

/**
 * [검사 - 검진기록 API]
 */
@WebMvcTest(controllers = CheckupController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CheckupControllerTest {

    private static final String XHR = "X-Requested-With";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CheckupService checkupService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private CheckupResponse response() {
        CheckupResponse response = new CheckupResponse();
        response.setCkupRcdSn(500L);
        response.setPetSn(10L);
        response.setSchdlSn(100L);
        response.setCkupYmd(LocalDate.of(2026, 11, 3));
        response.setHsptlNm("튼튼동물병원");
        response.setCkupTypeCd("HLTC0001");
        response.setCkupTypeCdNm("정기검진");
        return response;
    }

    @Test
    @WithMockLoginMember
    void 목록은_연도와_검진유형_조건을_받아_삭제된_일정_여부와_함께_돌려준다() throws Exception {
        CheckupSummary row = new CheckupSummary();
        row.setCkupRcdSn(500L);
        row.setHsptlNm("튼튼동물병원");
        row.setCkupTypeCdNm("예방접종");
        row.setSchdlDelYn(true);
        when(checkupService.search(eq(1L), eq(10L), any(), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 10, 1, 1, false));

        mockMvc.perform(get("/api/pets/10/checkups").param("year", "2026").param("ckupTypeCd", "HLTC0002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ckupTypeCdNm").value("예방접종"))
                .andExpect(jsonPath("$.data.content[0].schdlDelYn").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        verify(checkupService).search(eq(1L), eq(10L),
                argThat(cond -> Integer.valueOf(2026).equals(cond.getYear())
                        && "HLTC0002".equals(cond.getCkupTypeCd())),
                any());
    }

    @Test
    @WithMockLoginMember
    void 목록은_연도를_생략해도_조회한다() throws Exception {
        when(checkupService.search(eq(1L), eq(10L), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 10, 0, 0, false));

        mockMvc.perform(get("/api/pets/10/checkups"))
                .andExpect(status().isOk());
        verify(checkupService).search(eq(1L), eq(10L), argThat(cond -> cond.getYear() == null), any());
    }

    @Test
    @WithMockLoginMember
    void 등록은_201과_저장한_검진기록을_돌려준다() throws Exception {
        when(checkupService.create(eq(1L), eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/api/pets/10/checkups").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ckupYmd":"2026-11-03","hsptlNm":"튼튼동물병원","ckupTypeCd":"HLTC0001"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.ckupRcdSn").value(500))
                .andExpect(jsonPath("$.data.schdlSn").value(100));
    }

    @Test
    @WithMockLoginMember
    void 병원명이_81자면_400() throws Exception {
        String hospital = "가".repeat(81);
        mockMvc.perform(post("/api/pets/10/checkups").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ckupYmd\":\"2026-11-03\",\"hsptlNm\":\"" + hospital + "\",\"ckupTypeCd\":\"HLTC0001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(checkupService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 검진유형이나_검진일이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/checkups").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ckupYmd\":\"2026-11-03\",\"hsptlNm\":\"병원\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        mockMvc.perform(post("/api/pets/10/checkups").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hsptlNm\":\"병원\",\"ckupTypeCd\":\"HLTC0001\"}"))
                .andExpect(status().isBadRequest());
        verify(checkupService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 검진_내용이_500자_메모가_200자를_넘으면_400() throws Exception {
        String base = "{\"ckupYmd\":\"2026-11-03\",\"hsptlNm\":\"병원\",\"ckupTypeCd\":\"HLTC0001\",";
        mockMvc.perform(post("/api/pets/10/checkups").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(base + "\"ckupCn\":\"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/pets/10/checkups/500").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ckupMemoCn\":\"" + "가".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 수정은_200과_바뀐_검진기록을_돌려준다() throws Exception {
        when(checkupService.update(eq(1L), eq(10L), eq(500L), any())).thenReturn(response());

        mockMvc.perform(patch("/api/pets/10/checkups/500").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ckupMemoCn\":\"새 메모\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ckupRcdSn").value(500));
    }

    @Test
    @WithMockLoginMember
    void 삭제는_200을_돌려준다() throws Exception {
        mockMvc.perform(delete("/api/pets/10/checkups/500").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isOk());
        verify(checkupService).delete(1L, 10L, 500L);
    }
}
