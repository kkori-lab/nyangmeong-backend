package com.nyangmeong.community.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.community.service.CareMatchService;
import com.nyangmeong.community.service.CareRecruitmentService;
import com.nyangmeong.community.service.ChatRoomService;
import com.nyangmeong.community.service.CommunityAiCommentaryService;
import com.nyangmeong.community.service.LostReportService;
import com.nyangmeong.community.service.NewsService;
import com.nyangmeong.global.aitool.AiToolCatalogController;
import com.nyangmeong.global.aitool.AiToolCatalogService;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 커뮤니티 도메인 AI 도구 목록 추가분]
 * - 사이트 기능 전체 표의 읽기 후보에 붙인 @AiTool이 실리는지 (READ 7)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, CareMatchController.class, CareRecruitmentController.class,
        ChatRoomController.class, CommunityAiCommentaryController.class, LostReportController.class,
        NewsController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class CommunityReadAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    CareMatchService careMatchService;
    @MockitoBean
    CareRecruitmentService careRecruitmentService;
    @MockitoBean
    ChatRoomService chatRoomService;
    @MockitoBean
    CommunityAiCommentaryService communityAiCommentaryService;
    @MockitoBean
    LostReportService lostReportService;
    @MockitoBean
    NewsService newsService;

    private static String tool(String name) {
        return "$.data.tools[?(@.name == '" + name + "')]";
    }

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search(anyString())).thenReturn(List.of(summary));
    }

    @Test
    void 새_도구가_종류와_도메인과_함께_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("get_care_match") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_care_match") + ".domain").value("community"))
                .andExpect(jsonPath(tool("get_care_recruitment") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_care_recruitment") + ".domain").value("community"))
                .andExpect(jsonPath(tool("get_minimum_wage") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_minimum_wage") + ".domain").value("community"))
                .andExpect(jsonPath(tool("get_chat_unread_count") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_chat_unread_count") + ".domain").value("community"))
                .andExpect(jsonPath(tool("get_today_story") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_today_story") + ".domain").value("community"))
                .andExpect(jsonPath(tool("get_lost_report") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_lost_report") + ".domain").value("community"))
                .andExpect(jsonPath(tool("get_news") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_news") + ".domain").value("community"));
    }

    @Test
    void get_care_match_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_care_match");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/care-matches/{petPrtcMtchSn}"))
                .andExpect(jsonPath(t + ".params.petPrtcMtchSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petPrtcMtchSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void get_care_recruitment_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_care_recruitment");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/care-recruitments/{petPrtcRcrtSn}"))
                .andExpect(jsonPath(t + ".params.petPrtcRcrtSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petPrtcRcrtSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void get_minimum_wage_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_minimum_wage");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/care-recruitments/minimum-wage"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void get_chat_unread_count_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_chat_unread_count");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/chat-rooms/unread-count"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void get_today_story_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_today_story");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/community-ai-commentaries"))
                .andExpect(jsonPath(t + ".params.cmtryCrtrYmd.in").value("query"))
                .andExpect(jsonPath(t + ".params.cmtryCrtrYmd.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.cmtryCrtrYmd.type").value("date"))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void get_lost_report_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_lost_report");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/lost-reports/{lostTpofSn}"))
                .andExpect(jsonPath(t + ".params.lostTpofSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.lostTpofSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void get_news_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_news");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/news/{newsSn}"))
                .andExpect(jsonPath(t + ".params.newsSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.newsSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }
}
