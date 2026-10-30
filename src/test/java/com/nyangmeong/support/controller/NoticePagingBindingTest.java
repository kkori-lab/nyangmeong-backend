package com.nyangmeong.support.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import com.nyangmeong.support.dto.NoticeSearchCond;
import com.nyangmeong.support.service.NoticeService;

/**
 * [검사 - 공지사항 목록 페이지 쿼리 바인딩]
 * - page·size 가 PageQuery 로 service 까지 넘어가는지 (FN-CSC-001-04)
 */
@WebMvcTest(controllers = NoticeController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class NoticePagingBindingTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NoticeService noticeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void page와_size가_PageQuery로_넘어간다() throws Exception {
        when(noticeService.search(any(NoticeSearchCond.class), any()))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0, false));

        mockMvc.perform(get("/api/notices").param("page", "2").param("size", "5").param("latestYn", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> page = ArgumentCaptor.forClass(PageQuery.class);
        ArgumentCaptor<NoticeSearchCond> cond = ArgumentCaptor.forClass(NoticeSearchCond.class);
        verify(noticeService).search(cond.capture(), page.capture());
        assertThat(page.getValue().page()).isEqualTo(2);
        assertThat(page.getValue().size()).isEqualTo(5);
        assertThat(cond.getValue().getLatestYn()).isTrue();
    }
}
