package com.nyangmeong.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageQuery;
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
import com.nyangmeong.support.dto.FaqSearchCond;
import com.nyangmeong.support.dto.FaqSummary;
import com.nyangmeong.support.service.FaqService;

/**
 * [검사 - FAQ API]
 */
@WebMvcTest(controllers = FaqController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class FaqControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FaqService faqService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 목록은_다섯_필드와_조건_페이지를_바인딩한다() throws Exception {
        FaqSummary row = new FaqSummary();
        row.setFaqSn(7L);
        row.setFaqClsfCd("CSCC0002");
        row.setFaqClsfCdNm("계정");
        row.setQstnTtl("비밀번호를 잊어버렸어요");
        row.setAnsCn("재설정 메일을 보내 드려요.");
        when(faqService.search(any(FaqSearchCond.class), any(PageQuery.class)))
                .thenReturn(new PageResponse<>(List.of(row), 2, 10, 11, 2, false));

        mockMvc.perform(get("/api/faqs").param("faqClsfCd", "CSCC0002").param("keyword", "비밀번호")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].faqSn").value(7))
                .andExpect(jsonPath("$.data.content[0].faqClsfCd").value("CSCC0002"))
                .andExpect(jsonPath("$.data.content[0].faqClsfCdNm").value("계정"))
                .andExpect(jsonPath("$.data.content[0].qstnTtl").value("비밀번호를 잊어버렸어요"))
                .andExpect(jsonPath("$.data.content[0].ansCn").value("재설정 메일을 보내 드려요."));

        ArgumentCaptor<FaqSearchCond> condCaptor = ArgumentCaptor.forClass(FaqSearchCond.class);
        ArgumentCaptor<PageQuery> pageCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(faqService).search(condCaptor.capture(), pageCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(condCaptor.getValue().getFaqClsfCd()).isEqualTo("CSCC0002");
        org.assertj.core.api.Assertions.assertThat(condCaptor.getValue().getKeyword()).isEqualTo("비밀번호");
        org.assertj.core.api.Assertions.assertThat(pageCaptor.getValue().pageOrDefault()).isEqualTo(2);
    }

    @Test
    @WithMockLoginMember
    void 없는_카테고리_코드는_400() throws Exception {
        when(faqService.search(any(FaqSearchCond.class), any(PageQuery.class)))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/faqs").param("faqClsfCd", "XXXX"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_없이는_401() throws Exception {
        mockMvc.perform(get("/api/faqs"))
                .andExpect(status().isUnauthorized());
    }
}
