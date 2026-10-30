package com.nyangmeong.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import com.nyangmeong.support.dto.NoticeResponse;
import com.nyangmeong.support.dto.NoticeSearchCond;
import com.nyangmeong.support.dto.NoticeSummary;
import com.nyangmeong.support.service.NoticeService;

/**
 * [검사 - 공지사항 API]
 */
@WebMvcTest(controllers = NoticeController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class NoticeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NoticeService noticeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 목록은_검색어와_최신순_조건을_바인딩한다() throws Exception {
        NoticeSummary row = new NoticeSummary();
        row.setNtcMttrSn(3L);
        row.setNtcMttrTtl("점검 안내");
        row.setUpendFixYn(true);
        when(noticeService.search(any(NoticeSearchCond.class), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 5, 1, 1, false));

        mockMvc.perform(get("/api/notices").param("keyword", "점검").param("latestYn", "true").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ntcMttrSn").value(3))
                .andExpect(jsonPath("$.data.content[0].upendFixYn").value(true));

        ArgumentCaptor<NoticeSearchCond> captor = ArgumentCaptor.forClass(NoticeSearchCond.class);
        verify(noticeService).search(captor.capture(), any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getKeyword()).isEqualTo("점검");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getLatestYn()).isTrue();
    }

    @Test
    @WithMockLoginMember
    void 상세는_공지_번호로_조회한다() throws Exception {
        when(noticeService.getNotice(eq(3L)))
                .thenReturn(new NoticeResponse(3L, "점검 안내", "본문", LocalDateTime.of(2026, 10, 1, 9, 0)));

        mockMvc.perform(get("/api/notices/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ntcMttrSn").value(3))
                .andExpect(jsonPath("$.data.ntcMttrCn").value("본문"))
                .andExpect(jsonPath("$.data.inqCnt").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 없는_공지는_404() throws Exception {
        when(noticeService.getNotice(eq(9L))).thenThrow(new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(get("/api/notices/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 로그인_없이는_401() throws Exception {
        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isUnauthorized());
    }
}
