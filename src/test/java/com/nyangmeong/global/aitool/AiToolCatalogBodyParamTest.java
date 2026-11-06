package com.nyangmeong.global.aitool;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [검사 - 도구 목록 본문 칸 확장]
 * - 경로 변수와 같은 본문 칸 제외, multipart 파일 칸, List<record> 줄 칸, @AiGenerated
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogBodyParamTest.FakeBodyController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogBodyParamTest {

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
    void 경로_변수와_이름이_같은_본문_칸은_빼고_도구는_싣는다() throws Exception {
        givenCodes();
        String t = tool("move_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.memo.in").value("body"))
                .andExpect(jsonPath(t + ".call.contentType").doesNotExist());
    }

    @Test
    void multipart_요청은_파일_칸을_본문으로_싣고_contentType을_준다() throws Exception {
        givenCodes();
        String t = tool("upload_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.contentType").value(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(jsonPath(t + ".params.file.in").value("body"))
                .andExpect(jsonPath(t + ".params.file.type").value("file"))
                .andExpect(jsonPath(t + ".params.file.required").value(true))
                .andExpect(jsonPath(t + ".params.takenYmd.in").value("body"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"));
    }

    @Test
    void 줄_목록_칸은_array와_items로_싣고_중첩도_싣는다() throws Exception {
        givenCodes();
        String t = tool("lines_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.lines.type").value("array"))
                .andExpect(jsonPath(t + ".params.lines.required").value(true))
                .andExpect(jsonPath(t + ".params.lines.minItems").value(1))
                .andExpect(jsonPath(t + ".params.lines.items.amt.type").value("long"))
                .andExpect(jsonPath(t + ".params.lines.items.amt.required").value(true))
                .andExpect(jsonPath(t + ".params.lines.items.amt.in").doesNotExist())
                .andExpect(jsonPath(t + ".params.lines.items.kindCd.group").value("G1"))
                .andExpect(jsonPath(t + ".params.lines.items.kindCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.lines.items.subs.type").value("array"))
                .andExpect(jsonPath(t + ".params.lines.items.subs.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.lines.items.subs.minItems").value(2))
                .andExpect(jsonPath(t + ".params.lines.items.subs.items.nm.maxLength").value(10));
    }

    @Test
    void AiGenerated_칸은_generated를_싣고_필수로_싣지_않는다() throws Exception {
        givenCodes();
        String t = tool("lines_fake");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.requestId.generated").value("uuid"))
                .andExpect(jsonPath(t + ".params.requestId.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.requestId.maxLength").value(100))
                .andExpect(jsonPath(t + ".params.lines.items.lineId.generated").value("uuid"));
    }

    @Test
    void 값_목록과_쿼리의_파일_칸이_있는_도구는_뺀다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[*].name", not(hasItem("names_fake"))))
                .andExpect(jsonPath("$.data.tools[*].name", not(hasItem("query_file_fake"))))
                .andExpect(jsonPath("$.data.tools.length()").value(3));
    }

    record FakeMoveRequest(Long petSn, String memo) {
    }

    record FakeUploadRequest(@NotNull MultipartFile file, LocalDate takenYmd) {
    }

    record FakeSub(@Size(max = 10) String nm) {
    }

    record FakeLine(@NotNull Long amt, @AiCode("G1") String kindCd, @NotBlank @AiGenerated String lineId,
            @Size(min = 2) @Valid List<FakeSub> subs) {
    }

    record FakeLinesRequest(@NotBlank @Size(max = 100) @AiGenerated String requestId,
            @NotEmpty @Valid List<FakeLine> lines) {
    }

    record FakeNamesRequest(List<String> names) {
    }

    record FakeQueryFileCond(MultipartFile file) {
    }

    @RestController
    static class FakeBodyController {

        @AiTool(name = "move_fake", domain = "test", kind = AiToolKind.CARD)
        @PatchMapping("/test/body/{petSn}")
        public ResponseEntity<Void> move(@PathVariable Long petSn, @RequestBody FakeMoveRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "upload_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping(value = "/test/body/{petSn}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<Void> upload(@PathVariable Long petSn, @Valid @ModelAttribute FakeUploadRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "lines_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping("/test/body/lines")
        public ResponseEntity<Void> lines(@Valid @RequestBody FakeLinesRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "names_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping("/test/body/names")
        public ResponseEntity<Void> names(@RequestBody FakeNamesRequest req) {
            return ResponseEntity.ok().build();
        }

        @AiTool(name = "query_file_fake", domain = "test", kind = AiToolKind.READ)
        @GetMapping("/test/body/query-file")
        public ResponseEntity<Void> queryFile(@ModelAttribute FakeQueryFileCond cond) {
            return ResponseEntity.ok().build();
        }
    }
}
