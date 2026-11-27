package com.nyangmeong.global.aitool;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

/**
 * [검사 - /internal/ai/tools 쿼리 배열 칸]
 * - @RequestParam List<Long>·List<Integer> -> 쿼리 array + itemType, 필수면 minItems 1
 * - 그 밖의 목록 쿼리 칸(List<String>)이 있는 도구는 뺀다
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogQueryArrayParamTest.FakeQueryArrayController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogQueryArrayParamTest {

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
    void 필수_번호_목록_쿼리_칸은_array와_itemType_long으로_싣는다() throws Exception {
        String t = tool("delete_many_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".params.itemSn.in").value("query"))
                .andExpect(jsonPath(t + ".params.itemSn.type").value("array"))
                .andExpect(jsonPath(t + ".params.itemSn.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.itemSn.required").value(true))
                .andExpect(jsonPath(t + ".params.itemSn.minItems").value(1))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void 선택_정수_목록_쿼리_칸은_필수가_아니다() throws Exception {
        String t = tool("search_many_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.cnt.in").value("query"))
                .andExpect(jsonPath(t + ".params.cnt.type").value("array"))
                .andExpect(jsonPath(t + ".params.cnt.itemType").value("int"))
                .andExpect(jsonPath(t + ".params.cnt.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.cnt.minItems").doesNotExist());
    }

    @Test
    void 문자열_목록_쿼리_칸이_있는_도구는_싣지_않는다() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("search_names_fake"), hasSize(0)));
    }

    @RestController
    static class FakeQueryArrayController {

        @AiTool(name = "delete_many_fake", domain = "test", kind = AiToolKind.CARD)
        @DeleteMapping("/test/fake/items")
        public ResponseEntity<Void> deleteMany(@RequestParam("itemSn") List<Long> itemSns) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "search_many_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/fake/items")
        public ResponseEntity<Void> searchMany(@RequestParam(required = false) List<Integer> cnt) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "search_names_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/fake/names")
        public ResponseEntity<Void> searchNames(@RequestParam List<String> names) {
            return ResponseEntity.ok().build();
        }
    }
}
