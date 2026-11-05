package com.nyangmeong.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.ai.dto.AdminAiRouteCreateResponse;
import com.nyangmeong.ai.dto.AdminAiRouteSummary;
import com.nyangmeong.ai.service.AdminAiRouteService;
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
 * [검사 - 관리자 라우팅 매핑표 API]
 */
@WebMvcTest(controllers = AdminAiRouteController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminAiRouteControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String PATH = "/api/admin/ai/routes";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminAiRouteService adminAiRouteService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private AdminAiRouteSummary summary(boolean useYn) {
        return new AdminAiRouteSummary(7L, "HLT", "건강 일지", "SCR-HLT-002", null, "건강 일지, 오늘 기록", "AIMC0032",
                false, 1, null, useYn, LocalDateTime.of(2026, 11, 5, 12, 0));
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_403_ADMIN_ONLY() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 매핑표를_true_false로_돌려준다() throws Exception {
        when(adminAiRouteService.search("병원")).thenReturn(List.of(summary(true)));

        mockMvc.perform(get(PATH).param("q", "병원"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].aiRoutMpngSn").value(7))
                .andExpect(jsonPath("$.data[0].rprsChipYn").value(false))
                .andExpect(jsonPath("$.data[0].useYn").value(true))
                .andExpect(jsonPath("$.data[0].prrtSeq").value(1));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 등록하면_201과_번호를_돌려준다() throws Exception {
        when(adminAiRouteService.create(eq(9L), any())).thenReturn(new AdminAiRouteCreateResponse(50L));

        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trgtScrnNm\":\"건강 일지\",\"scrnId\":\"SCR-HLT-002\",\"instKwrdCn\":\"영수증\","
                        + "\"mvmnMthdCd\":\"AIMC0032\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aiRoutMpngSn").value(50));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 대상_화면명이_100자를_넘으면_400() throws Exception {
        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trgtScrnNm\":\"" + "가".repeat(101) + "\",\"mvmnMthdCd\":\"AIMC0035\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("trgtScrnNm"));
        verify(adminAiRouteService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 우선순위가_0이면_400() throws Exception {
        mockMvc.perform(patch(PATH + "/7").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"prrtSeq\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("prrtSeq"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 수정하면_바뀐_행을_돌려준다() throws Exception {
        when(adminAiRouteService.update(eq(9L), eq(7L), any())).thenReturn(summary(false));

        mockMvc.perform(patch(PATH + "/7").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"useYn\":false,\"prrtSeq\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.useYn").value(false));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_매핑_삭제는_404() throws Exception {
        doThrow(new CustomException(ErrorCode.AI_ROUTE_NOT_FOUND)).when(adminAiRouteService).delete(9L, 99L);

        mockMvc.perform(delete(PATH + "/99").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_ROUTE_NOT_FOUND"));
    }
}
