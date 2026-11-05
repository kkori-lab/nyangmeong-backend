package com.nyangmeong.global.aitool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.SecurityPathProperties;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - /internal/ai/tools 도구 목록 수집]
 * - 가짜 컨트롤러로 칸·필수·규칙·제외·해시 확인
 * - 열린 경로는 로컬 application.properties와 무관하게 고정
 * - 테스트 안 컨트롤러는 스캔에서 빠져 @Import로 등록
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogControllerTest.FakeToolController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogControllerTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private static CommonCodeSummary code(String comCd, String comCdNm) {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd(comCd);
        summary.setComCdNm(comCdNm);
        return summary;
    }

    private void givenCodes(String g2Name) {
        when(commonCodeService.search("G1")).thenReturn(List.of(code("C0001", "가")));
        when(commonCodeService.search("G2")).thenReturn(List.of(code("C0002", g2Name), code("C0003", "다")));
    }

    private JsonNode catalog() throws Exception {
        String body = mockMvc.perform(get(PATH)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readTree(body).path("data");
    }

    @Test
    void 기본_열린_경로에_도구_목록이_있다() {
        assertThat(new SecurityPathProperties().getPermitAllPaths()).contains(PATH);
    }

    @Test
    void 쿠키가_없어도_200이고_다른_internal_경로는_401이다() throws Exception {
        givenCodes("나");
        mockMvc.perform(get(PATH)).andExpect(status().isOk());
        mockMvc.perform(get("/internal/ai/conversations")).andExpect(status().isUnauthorized());
    }

    @Test
    void 경로_쿼리_칸과_공통코드를_준다() throws Exception {
        givenCodes("나");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].kind").value("READ"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].call.method").value("GET"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].call.path").value("/test/fake/{petSn}"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.petSn.in").value("path"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.petSn.type").value("long"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.petSn.required").value(true))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.kindCd.in").value("query"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.kindCd.type").value("code"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.kindCd.group").value("G1"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.kindCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.kindCd.codes[0].nm").value("가"))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.kindCd.required").doesNotExist())
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.page.required").value(true))
                .andExpect(jsonPath("$.data.tools[?(@.name == 'read_fake')].params.size.required").doesNotExist());
    }

    @Test
    void 요청_본문_칸의_검증_어노테이션과_규칙을_준다() throws Exception {
        givenCodes("나");
        String card = "$.data.tools[?(@.name == 'card_fake')]";
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(card + ".kind").value("CARD"))
                .andExpect(jsonPath(card + ".params.a.in").value("body"))
                .andExpect(jsonPath(card + ".params.a.required").value(true))
                .andExpect(jsonPath(card + ".params.b.type").value("string"))
                .andExpect(jsonPath(card + ".params.b.minLength").value(1))
                .andExpect(jsonPath(card + ".params.b.maxLength").value(10))
                .andExpect(jsonPath(card + ".params.amt.type").value("decimal"))
                .andExpect(jsonPath(card + ".params.amt.precision").value(7))
                .andExpect(jsonPath(card + ".params.amt.scale").value(2))
                .andExpect(jsonPath(card + ".params.cnt.positive").value(true))
                .andExpect(jsonPath(card + ".params.digitsOnly.pattern").value("^[0-9]+$"))
                .andExpect(jsonPath(card + ".params.typeCd.codes[1].cd").value("C0003"))
                .andExpect(jsonPath(card + ".params.at.type").value("datetime"))
                .andExpect(jsonPath(card + ".rules[0].requiredIf.field").value("b"))
                .andExpect(jsonPath(card + ".rules[0].requiredIf.when").value("typeCd"))
                .andExpect(jsonPath(card + ".rules[0].requiredIf.equals").value("C0002"))
                .andExpect(jsonPath(card + ".rules[1].oneOf[0]").value("a"))
                .andExpect(jsonPath(card + ".rules[1].oneOf[1]").value("cnt"));
    }

    @Test
    void 지원하지_않는_칸이나_없는_칸의_규칙이_있는_도구와_표시_없는_API는_뺀다() throws Exception {
        givenCodes("나");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[*].name", not(hasItem("list_fake"))))
                .andExpect(jsonPath("$.data.tools[*].name", not(hasItem("search_fake"))))
                .andExpect(jsonPath("$.data.tools[*].name", not(hasItem("bad_rule_fake"))))
                .andExpect(jsonPath("$.data.tools.length()").value(2));
    }

    @Test
    void 코드_값만_바뀌면_version만_바뀌고_structureVersion은_같다() throws Exception {
        givenCodes("나");
        JsonNode first = catalog();
        JsonNode again = catalog();
        givenCodes("나나");
        JsonNode changed = catalog();

        assertThat(again.path("version").asString()).isEqualTo(first.path("version").asString());
        assertThat(changed.path("version").asString()).isNotEqualTo(first.path("version").asString());
        assertThat(changed.path("structureVersion").asString())
                .isEqualTo(first.path("structureVersion").asString());
        assertThat(first.path("version").asString()).hasSize(64);
    }

    @AiRequiredIf(field = "b", when = "typeCd", equals = "C0002")
    @AiOneOf({"a", "cnt"})
    record FakeCreateRequest(@NotNull Long a, @Size(min = 1, max = 10) String b,
            @Digits(integer = 5, fraction = 2) BigDecimal amt, @Positive Long cnt,
            @Pattern(regexp = "^[0-9]+$") String digitsOnly, @AiCode("G2") String typeCd,
            java.time.LocalDateTime at) {
    }

    record FakeListRequest(List<String> names) {
    }

    @AiRequiredIf(field = "missing", when = "a", equals = "X")
    record FakeBadRuleRequest(Long a) {
    }

    record FakeSearchCond(String q) {
    }

    @RestController
    static class FakeToolController {

        @AiTool(name = "read_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/fake/{petSn}")
        public ResponseEntity<Void> read(@PathVariable Long petSn,
                @RequestParam(required = false) @AiCode("G1") String kindCd, @RequestParam Integer page,
                @RequestParam(defaultValue = "10") Integer size) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "card_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping("/test/fake")
        public ResponseEntity<Void> create(@Valid @RequestBody FakeCreateRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "list_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping("/test/fake/list")
        public ResponseEntity<Void> createList(@RequestBody FakeListRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "bad_rule_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping("/test/fake/bad-rule")
        public ResponseEntity<Void> createBadRule(@RequestBody FakeBadRuleRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "search_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/fake")
        public ResponseEntity<Void> search(@ModelAttribute FakeSearchCond cond) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/test/fake/plain")
        public ResponseEntity<Void> plain() {
            return ResponseEntity.ok().build();
        }
    }
}
