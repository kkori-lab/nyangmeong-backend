package com.nyangmeong.place.controller;

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
import com.nyangmeong.place.service.PlaceBookmarkService;
import com.nyangmeong.place.service.PlaceService;

/**
 * [검사 - 플레이스 도메인 AI 도구 목록]
 * - 실제 플레이스 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 2)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, PlaceController.class, PlaceBookmarkController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class PlaceAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    PlaceService placeService;
    @MockitoBean
    PlaceBookmarkService placeBookmarkService;

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
    void 플레이스_READ_2개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(2))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("list_place_bookmarks",
                        "list_places")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(2)))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'place')].name", hasSize(2)));
    }

    @Test
    void 장소_목록_도구는_분류와_지도_범위가_필수다() throws Exception {
        givenCodes();
        String t = tool("list_places");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.plcClsfCd.group").value("PLCG01"))
                .andExpect(jsonPath(t + ".params.plcClsfCd.required").value(true))
                .andExpect(jsonPath(t + ".params.swLat.type").value("decimal"))
                .andExpect(jsonPath(t + ".params.swLat.required").value(true))
                .andExpect(jsonPath(t + ".params.neLot.required").value(true))
                .andExpect(jsonPath(t + ".params.cntrLat.required").value(true))
                .andExpect(jsonPath(t + ".params.keyword.maxLength").value(100));
    }

    @Test
    void 즐겨찾기_도구는_분류가_필수_코드다() throws Exception {
        givenCodes();
        String t = tool("list_place_bookmarks");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.plcClsfCd.in").value("query"))
                .andExpect(jsonPath(t + ".params.plcClsfCd.group").value("PLCG01"))
                .andExpect(jsonPath(t + ".params.plcClsfCd.required").value(true));
    }
}
