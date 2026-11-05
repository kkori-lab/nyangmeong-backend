package com.nyangmeong.global.aitool;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [검사 - /internal/ai/tools 쿼리 칸 수집]
 * - @ModelAttribute 검색 조건 클래스 필드, 경로 변수와 같은 이름 필드 제외, PageQuery page·size
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogQueryParamTest.FakeSearchController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogQueryParamTest {

    private static final String PATH = "/internal/ai/tools";
    private static final String TOOL = "$.data.tools[?(@.name == 'search_class_fake')]";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search("G1")).thenReturn(List.of(summary));
    }

    @Test
    void 검색_조건_클래스_필드를_쿼리_칸으로_싣는다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".params.fromYmd.in").value("query"))
                .andExpect(jsonPath(TOOL + ".params.fromYmd.type").value("date"))
                .andExpect(jsonPath(TOOL + ".params.fromYmd.required").doesNotExist())
                .andExpect(jsonPath(TOOL + ".params.itemSn.required").value(true))
                .andExpect(jsonPath(TOOL + ".params.kindCd.type").value("code"))
                .andExpect(jsonPath(TOOL + ".params.kindCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(TOOL + ".params.cnt.min").value(1))
                .andExpect(jsonPath(TOOL + ".params.cnt.max").value(30));
    }

    @Test
    void 경로_변수와_이름이_같은_필드는_경로_칸만_남고_static_필드는_싣지_않는다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(TOOL + ".params.petSn.required").value(true))
                .andExpect(jsonPath(TOOL + ".params.LIMIT").doesNotExist());
    }

    @Test
    void PageQuery는_page와_size만_싣는다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".params.page.in").value("query"))
                .andExpect(jsonPath(TOOL + ".params.page.type").value("int"))
                .andExpect(jsonPath(TOOL + ".params.page.min").value(1))
                .andExpect(jsonPath(TOOL + ".params.page.required").doesNotExist())
                .andExpect(jsonPath(TOOL + ".params.size.min").value(1))
                .andExpect(jsonPath(TOOL + ".params.size.max").value(50))
                .andExpect(jsonPath(TOOL + ".params.sort").doesNotExist());
    }

    @Data
    public static class FakeClassCond {
        static final int LIMIT = 10;

        private Long petSn;
        private LocalDate fromYmd;
        @NotNull
        private Long itemSn;
        @AiCode("G1")
        private String kindCd;
        @Min(1)
        @Max(30)
        private Integer cnt;
    }

    @RestController
    static class FakeSearchController {

        @AiTool(name = "search_class_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/fake/{petSn}/items")
        public ResponseEntity<Void> search(@PathVariable Long petSn, @ModelAttribute FakeClassCond cond,
                PageQuery pq) {
            return ResponseEntity.ok().build();
        }
    }
}
