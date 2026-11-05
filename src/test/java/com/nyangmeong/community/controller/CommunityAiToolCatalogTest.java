package com.nyangmeong.community.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.nyangmeong.community.service.CareRecruitmentService;
import com.nyangmeong.community.service.CommentService;
import com.nyangmeong.community.service.LostReportService;
import com.nyangmeong.community.service.NewsService;
import com.nyangmeong.community.service.PostService;

/**
 * [검사 - 커뮤니티 도메인 AI 도구 목록]
 * - 실제 커뮤니티 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 8)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, PostController.class, CommentController.class,
        LostReportController.class, CareRecruitmentController.class, NewsController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class CommunityAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    PostService postService;
    @MockitoBean
    CommentService commentService;
    @MockitoBean
    LostReportService lostReportService;
    @MockitoBean
    CareRecruitmentService careRecruitmentService;
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
    void 커뮤니티_READ_8개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(8))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("get_popular_photo", "get_post",
                        "list_care_recruitments", "list_comments", "list_health_tips", "list_lost_reports",
                        "list_news", "list_posts")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(8)))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'community')].name", hasSize(8)));
    }

    @Test
    void 글_목록_도구는_게시판_코드와_페이지가_있다() throws Exception {
        givenCodes();
        String t = tool("list_posts");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.bbsCd.group").value("CMUG01"))
                .andExpect(jsonPath(t + ".params.keyword.type").value("string"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"));
    }

    @Test
    void 실종_목록_도구는_상태와_동물_구분이_코드다() throws Exception {
        givenCodes();
        String t = tool("list_lost_reports");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.lostTpofSttsCd.group").value("CMUG03"))
                .andExpect(jsonPath(t + ".params.petSeCd.group").value("PETG01"))
                .andExpect(jsonPath(t + ".params.wtnsPrd.type").value("string"))
                .andExpect(jsonPath(t + ".params.wtnsPrd.group").doesNotExist())
                .andExpect(jsonPath(t + ".params.rgnSn.type").value("long"))
                .andExpect(jsonPath(t + ".params.mineYn.type").value("boolean"));
    }

    @Test
    void 돌봄_모집_도구는_모집_상태가_코드다() throws Exception {
        givenCodes();
        String t = tool("list_care_recruitments");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.rcrtSttsCd.group").value("PCAG01"))
                .andExpect(jsonPath(t + ".params.petSeCd.group").value("PETG01"))
                .andExpect(jsonPath(t + ".params.prtcBgngDe.type").value("date"))
                .andExpect(jsonPath(t + ".params.petSizeCd.group").doesNotExist());
    }

    @Test
    void 뉴스_도구는_분류가_코드다() throws Exception {
        givenCodes();
        String t = tool("list_news");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.newsClsfCd.group").value("CMUG06"));
    }

    @Test
    void 댓글_도구는_글_번호가_경로다() throws Exception {
        givenCodes();
        String t = tool("list_comments");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/posts/{pstSn}/comments"))
                .andExpect(jsonPath(t + ".params.pstSn.in").value("path"));
    }
}
