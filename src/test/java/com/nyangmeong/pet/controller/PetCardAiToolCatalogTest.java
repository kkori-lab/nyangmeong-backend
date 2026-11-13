package com.nyangmeong.pet.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 반려동물 도메인 AI 쓰기 도구]
 * - 반려동물 수정이 CARD로 칸·코드와 함께 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, PetController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class PetCardAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    PetService petService;

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
    void 반려동물_수정_도구는_PATCH이고_코드_칸을_준다() throws Exception {
        givenCodes();
        String t = tool("draft_pet_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".domain").value("pet"))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSeCd.group").value("PETG01"))
                .andExpect(jsonPath(t + ".params.petGndrCd.group").value("PETG02"))
                .andExpect(jsonPath(t + ".params.colrCd.group").value("PETG03"))
                .andExpect(jsonPath(t + ".params.colrCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.petBdwg.positive").value(true))
                .andExpect(jsonPath(t + ".params.petBdwg.scale").value(2))
                .andExpect(jsonPath(t + ".params.chipNo.pattern").value("^[0-9]{0,20}$"))
                .andExpect(jsonPath(t + ".params.goalBdwgReset.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.petNm.required").doesNotExist());
    }
}
