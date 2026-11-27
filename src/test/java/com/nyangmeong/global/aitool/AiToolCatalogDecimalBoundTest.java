package com.nyangmeong.global.aitool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * [검사 - /internal/ai/tools 소수 최소·최대값]
 * - @DecimalMin·@DecimalMax는 경계를 포함하고 정수 값일 때만 min·max, @Min·@Max가 있으면 그것을 쓴다
 */
@WebMvcTest(controllers = AiToolCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class,
        AiToolCatalogDecimalBoundTest.FakeDecimalController.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class AiToolCatalogDecimalBoundTest {

    private static final String PATH = "/internal/ai/tools";
    private static final String TOOL = "$.data.tools[?(@.name == 'decimal_fake')]";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 정수_경계값은_min과_max로_싣는다() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".params.lat.min").value(-90))
                .andExpect(jsonPath(TOOL + ".params.lat.max").value(90))
                .andExpect(jsonPath(TOOL + ".params.weight.max").value(200))
                .andExpect(jsonPath(TOOL + ".params.weight.min").doesNotExist());
    }

    @Test
    void 소수_경계값과_경계를_포함하지_않는_값은_싣지_않는다() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".params.rate.min").doesNotExist())
                .andExpect(jsonPath(TOOL + ".params.rate.max").doesNotExist())
                .andExpect(jsonPath(TOOL + ".params.open.min").doesNotExist())
                .andExpect(jsonPath(TOOL + ".params.open.max").doesNotExist());
    }

    @Test
    void Min이_있으면_DecimalMin보다_먼저_쓴다() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".params.both.min").value(5));
    }

    record FakeDecimalRequest(@DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @DecimalMax("200.00") BigDecimal weight,
            @DecimalMin("0.5") @DecimalMax("9.5") BigDecimal rate,
            @DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "10", inclusive = false) BigDecimal open,
            @Min(5) @DecimalMin("1") BigDecimal both) {
    }

    @RestController
    static class FakeDecimalController {

        @AiTool(name = "decimal_fake", domain = "test", kind = AiToolKind.CARD)
        @PostMapping("/test/decimal")
        public ResponseEntity<Void> save(@RequestBody FakeDecimalRequest req) {
            return ResponseEntity.ok().build();
        }
    }
}
