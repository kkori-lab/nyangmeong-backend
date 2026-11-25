package com.nyangmeong.global.aitool;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

import jakarta.validation.constraints.Size;

/**
 * [검사 - 도구 목록 칸 제외 @AiExclude]
 * - 싣지 못하는 모양(중첩 객체·값 목록)의 칸에 붙이면 그 칸만 빠지고 도구는 실린다
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogExcludeTest.FakeExcludeController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogExcludeTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

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
    void 본문의_AiExclude_칸만_빼고_도구는_싣는다() throws Exception {
        givenCodes();
        String t = tool("exclude_body_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.memo.maxLength").value(10))
                .andExpect(jsonPath(t + ".params.range").doesNotExist())
                .andExpect(jsonPath(t + ".params.names").doesNotExist());
    }

    @Test
    void 검색_조건의_AiExclude_칸도_뺀다() throws Exception {
        givenCodes();
        String t = tool("exclude_query_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.keyword.in").value("query"))
                .andExpect(jsonPath(t + ".params.tags").doesNotExist());
    }

    @Test
    void 제외_칸이_있어도_도구_수는_그대로다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(2));
    }

    record FakeRange(Long from, Long to) {
    }

    record FakeExcludeRequest(@Size(max = 10) String memo, @AiExclude FakeRange range,
            @AiExclude List<String> names) {
    }

    record FakeExcludeCond(String keyword, @AiExclude List<String> tags) {
    }

    @RestController
    static class FakeExcludeController {

        @AiTool(name = "exclude_body_fake", domain = "test", kind = AiToolKind.CARD)
        @PatchMapping("/test/exclude")
        public ResponseEntity<Void> body(@RequestBody FakeExcludeRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "exclude_query_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/exclude")
        public ResponseEntity<Void> query(@ModelAttribute FakeExcludeCond cond) {
            return ResponseEntity.ok().build();
        }
    }
}
