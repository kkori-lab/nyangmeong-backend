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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.ai.dto.AdminAiKeywordCreateResponse;
import com.nyangmeong.ai.dto.AdminAiKeywordSummary;
import com.nyangmeong.ai.service.AdminAiKeywordService;
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
 * [검사 - 관리자 키워드 사전 API]
 */
@WebMvcTest(controllers = AdminAiKeywordController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminAiKeywordControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String PATH = "/api/admin/ai/keywords";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminAiKeywordService adminAiKeywordService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private AdminAiKeywordSummary summary(boolean useYn) {
        return new AdminAiKeywordSummary(7L, "AIMC0038", "가계부 카테고리", "사료", "사료, 건사료, 습식",
                "가계부 카테고리 = 사료(MYPG05)", "MYPC0013", "사료", 20, null, useYn);
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_403_ADMIN_ONLY() throws Exception {
        mockMvc.perform(get(PATH).param("dictUsgCd", "AIMC0038"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 키워드_목록을_코드_이름과_true_false로_돌려준다() throws Exception {
        when(adminAiKeywordService.search("AIMC0038", "가계부 카테고리")).thenReturn(List.of(summary(true)));

        mockMvc.perform(get(PATH).param("dictUsgCd", "AIMC0038").param("kwrdClsfNm", "가계부 카테고리"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].kwrdDictSn").value(7))
                .andExpect(jsonPath("$.data[0].mpngCd").value("MYPC0013"))
                .andExpect(jsonPath("$.data[0].mpngCdNm").value("사료"))
                .andExpect(jsonPath("$.data[0].useYn").value(true));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 용도_없이_조회하면_400() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isBadRequest());
        verify(adminAiKeywordService, never()).search(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 등록하면_201과_번호를_돌려준다() throws Exception {
        when(adminAiKeywordService.create(eq(9L), any())).thenReturn(new AdminAiKeywordCreateResponse(70L));

        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dictUsgCd\":\"AIMC0038\",\"kwrdClsfNm\":\"가계부 카테고리\",\"artclNm\":\"간식\","
                        + "\"kwrdCn\":\"간식, 츄르\",\"mpngCd\":\"MYPC0014\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.kwrdDictSn").value(70));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 항목명이_100자를_넘으면_400() throws Exception {
        mockMvc.perform(post(PATH).header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dictUsgCd\":\"AIMC0037\",\"kwrdClsfNm\":\"날짜\",\"artclNm\":\"" + "가".repeat(101)
                        + "\",\"kwrdCn\":\"오늘\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("artclNm"));
        verify(adminAiKeywordService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 수정하면_바뀐_행을_돌려준다() throws Exception {
        when(adminAiKeywordService.update(eq(9L), eq(7L), any())).thenReturn(summary(false));

        mockMvc.perform(patch(PATH + "/7").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"useYn\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.useYn").value(false));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_키워드_삭제는_404() throws Exception {
        doThrow(new CustomException(ErrorCode.AI_KEYWORD_NOT_FOUND)).when(adminAiKeywordService).delete(9L, 99L);

        mockMvc.perform(delete(PATH + "/99").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_KEYWORD_NOT_FOUND"));
    }
}
