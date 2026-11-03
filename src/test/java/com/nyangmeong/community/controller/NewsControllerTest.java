package com.nyangmeong.community.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.nyangmeong.community.dto.NewsResponse;
import com.nyangmeong.community.dto.NewsSearchCond;
import com.nyangmeong.community.dto.NewsSummary;
import com.nyangmeong.community.service.NewsService;
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

/**
 * [검사 - 뉴스 API]
 */
@WebMvcTest(controllers = NewsController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class NewsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NewsService newsService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 목록은_필드와_조건_페이지를_바인딩한다() throws Exception {
        NewsSummary row = new NewsSummary();
        row.setNewsSn(7L);
        row.setNewsClsfCd("CMUC0018");
        row.setNewsSrcCd("CMUC0022");
        row.setNewsSrcCdNm("외부 언론사");
        row.setSrcPressNm("전북일보");
        row.setNewsTtl("전북 유기동물 입양 캠페인 확대");
        row.setNewsCn("입양률을 높이기 위한 캠페인");
        row.setPblcnDt(LocalDateTime.of(2026, 11, 2, 10, 0));
        when(newsService.search(any(NewsSearchCond.class), any(PageQuery.class)))
                .thenReturn(new PageResponse<>(List.of(row), 2, 10, 11, 2, false));

        mockMvc.perform(get("/api/news").param("newsClsfCd", "CMUC0018").param("keyword", "입양")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].newsSn").value(7))
                .andExpect(jsonPath("$.data.content[0].newsClsfCd").value("CMUC0018"))
                .andExpect(jsonPath("$.data.content[0].newsSrcCdNm").value("외부 언론사"))
                .andExpect(jsonPath("$.data.content[0].srcPressNm").value("전북일보"))
                .andExpect(jsonPath("$.data.content[0].newsTtl").value("전북 유기동물 입양 캠페인 확대"))
                .andExpect(jsonPath("$.data.content[0].newsCn").value("입양률을 높이기 위한 캠페인"))
                .andExpect(jsonPath("$.data.content[0].pblcnDt").exists());

        ArgumentCaptor<NewsSearchCond> condCaptor = ArgumentCaptor.forClass(NewsSearchCond.class);
        ArgumentCaptor<PageQuery> pageCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(newsService).search(condCaptor.capture(), pageCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(condCaptor.getValue().getNewsClsfCd()).isEqualTo("CMUC0018");
        org.assertj.core.api.Assertions.assertThat(condCaptor.getValue().getKeyword()).isEqualTo("입양");
        org.assertj.core.api.Assertions.assertThat(pageCaptor.getValue().pageOrDefault()).isEqualTo(2);
    }

    @Test
    @WithMockLoginMember
    void 없는_카테고리_코드는_400() throws Exception {
        when(newsService.search(any(NewsSearchCond.class), any(PageQuery.class)))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/news").param("newsClsfCd", "XXXX"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 상세는_원문_링크까지_돌려준다() throws Exception {
        NewsResponse news = new NewsResponse();
        news.setNewsSn(7L);
        news.setNewsSrcCd("CMUC0022");
        news.setNewsSrcCdNm("외부 언론사");
        news.setSrcPressNm("전북일보");
        news.setNewsTtl("전북 유기동물 입양 캠페인 확대");
        news.setNewsCn("요약");
        news.setOrgtxtPath("https://news.google.com/rss/articles/abc");
        when(newsService.getNews(7L)).thenReturn(news);

        mockMvc.perform(get("/api/news/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newsSn").value(7))
                .andExpect(jsonPath("$.data.srcPressNm").value("전북일보"))
                .andExpect(jsonPath("$.data.newsCn").value("요약"))
                .andExpect(jsonPath("$.data.orgtxtPath").value("https://news.google.com/rss/articles/abc"));
    }

    @Test
    @WithMockLoginMember
    void 없는_뉴스는_404() throws Exception {
        when(newsService.getNews(99L)).thenThrow(new CustomException(ErrorCode.NEWS_NOT_FOUND));

        mockMvc.perform(get("/api/news/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 로그인_없이는_401() throws Exception {
        mockMvc.perform(get("/api/news"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/news/7"))
                .andExpect(status().isUnauthorized());
    }
}
