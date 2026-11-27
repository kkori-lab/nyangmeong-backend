package com.nyangmeong.global.aitool;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.code.CommonCodeService;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * [검사 - 도구 목록 enum 칸]
 * - enum 칸 -> string + values, List<enum> -> array + itemType string + values, List<String> 도구는 뺌
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogEnumParamTest.FakeEnumController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogEnumParamTest {

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

    @Test
    void enum_본문_칸은_string과_values로_싣는다() throws Exception {
        String t = tool("enum_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.color.type").value("string"))
                .andExpect(jsonPath(t + ".params.color.required").value(true))
                .andExpect(jsonPath(t + ".params.color.values[*]").value(contains("RED", "GREEN", "BLUE")))
                .andExpect(jsonPath(t + ".params.color.codes").doesNotExist())
                .andExpect(jsonPath(t + ".params.memo.values").doesNotExist());
    }

    @Test
    void enum_목록_칸은_array와_itemType_string과_values로_싣는다() throws Exception {
        String t = tool("enum_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.colors.type").value("array"))
                .andExpect(jsonPath(t + ".params.colors.required").value(true))
                .andExpect(jsonPath(t + ".params.colors.minItems").value(1))
                .andExpect(jsonPath(t + ".params.colors.itemType").value("string"))
                .andExpect(jsonPath(t + ".params.colors.values[*]").value(contains("RED", "GREEN", "BLUE")))
                .andExpect(jsonPath(t + ".params.colors.items").doesNotExist());
    }

    @Test
    void enum_쿼리_칸도_values로_싣는다() throws Exception {
        String t = tool("enum_query_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.color.in").value("query"))
                .andExpect(jsonPath(t + ".params.color.type").value("string"))
                .andExpect(jsonPath(t + ".params.color.values[*]").value(contains("RED", "GREEN", "BLUE")));
    }

    @Test
    void 문자열_목록_칸이_있는_도구는_여전히_뺀다() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[*].name", not(hasItem("string_list_fake"))));
    }

    @Test
    void 번호_목록_칸에는_values를_싣지_않는다() throws Exception {
        String t = tool("enum_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.sns.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.sns.values").doesNotExist());
    }

    enum FakeColor {
        RED, GREEN, BLUE
    }

    record EnumBody(@NotNull FakeColor color, @NotEmpty List<@NotNull FakeColor> colors, List<Long> sns,
            String memo) {
    }

    record StringListBody(List<String> names) {
    }

    @RestController
    static class FakeEnumController {

        @AiTool(name = "enum_fake", domain = "fake", kind = AiToolKind.CARD)
        @PutMapping("/api/fake/enums")
        ResponseEntity<Void> enums(@Valid @RequestBody EnumBody body) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "enum_query_fake", domain = "fake", kind = AiToolKind.READ)
        @GetMapping("/api/fake/enums")
        ResponseEntity<Void> enumQuery(@RequestParam(required = false) FakeColor color) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "string_list_fake", domain = "fake", kind = AiToolKind.CARD)
        @PostMapping("/api/fake/string-lists")
        ResponseEntity<Void> stringList(@Valid @RequestBody StringListBody body) {
            return ResponseEntity.ok().build();
        }
    }
}
