package com.nyangmeong.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import com.nyangmeong.support.dto.AdminFaqResponse;
import com.nyangmeong.support.service.AdminFaqService;

/**
 * [검사 - 관리자 FAQ API]
 */
@WebMvcTest(controllers = AdminFaqController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminFaqControllerTest {

    private static final String ADMIN = "USRC0005";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminFaqService adminFaqService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private AdminFaqResponse response(boolean useYn) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 3, 10, 0);
        return new AdminFaqResponse(4L, "CSCC0002", "비밀번호를 잊었어요", "답변", 1, useYn, now, now);
    }

    private String body(String qstnTtl, String ansCn) {
        return "{\"faqClsfCd\":\"CSCC0002\",\"qstnTtl\":\"" + qstnTtl + "\",\"ansCn\":\"" + ansCn
                + "\",\"sortSeq\":1,\"useYn\":true}";
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_403_ADMIN_ONLY() throws Exception {
        mockMvc.perform(get("/api/admin/faqs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_FAQ_상세는_404() throws Exception {
        when(adminFaqService.get(99L)).thenThrow(new CustomException(ErrorCode.FAQ_NOT_FOUND));

        mockMvc.perform(get("/api/admin/faqs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAQ_NOT_FOUND"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 등록하면_201과_FAQ를_돌려준다() throws Exception {
        when(adminFaqService.create(eq(9L), any())).thenReturn(response(true));

        mockMvc.perform(post("/api/admin/faqs").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("비밀번호를 잊었어요", "가".repeat(4000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.faqSn").value(4))
                .andExpect(jsonPath("$.data.useYn").value(true));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 질문이_200자를_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/admin/faqs").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("가".repeat(201), "답변")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("qstnTtl"));
        verify(adminFaqService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 답변이_4000자를_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/admin/faqs").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(body("질문", "가".repeat(4001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("ansCn"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 정렬_순서가_음수면_400() throws Exception {
        mockMvc.perform(post("/api/admin/faqs").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"faqClsfCd\":\"CSCC0002\",\"qstnTtl\":\"질문\",\"ansCn\":\"답변\",\"sortSeq\":-1,"
                        + "\"useYn\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("sortSeq"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 사용_여부를_끄면_비노출로_돌려준다() throws Exception {
        when(adminFaqService.update(eq(9L), eq(4L), any())).thenReturn(response(false));

        mockMvc.perform(patch("/api/admin/faqs/4").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"useYn\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.useYn").value(false));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 수정에서_공백_질문은_400() throws Exception {
        mockMvc.perform(patch("/api/admin/faqs/4").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"qstnTtl\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("qstnTtl"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_FAQ_수정은_404() throws Exception {
        when(adminFaqService.update(eq(9L), eq(99L), any())).thenThrow(new CustomException(ErrorCode.FAQ_NOT_FOUND));

        mockMvc.perform(patch("/api/admin/faqs/99").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"useYn\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAQ_NOT_FOUND"));
    }
}
