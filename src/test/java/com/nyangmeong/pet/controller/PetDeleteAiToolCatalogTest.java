package com.nyangmeong.pet.controller;

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
import com.nyangmeong.pet.service.CaregiverInvitationService;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 반려동물 AI 삭제 도구]
 * - 반려동물 삭제·공동양육 연동 해제·취소한 초대 삭제가 DELETE에 경로 칸만으로 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, PetController.class, CaregiverInvitationController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class PetDeleteAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    PetService petService;
    @MockitoBean
    CaregiverInvitationService caregiverInvitationService;

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
    void 반려동물_삭제_도구는_DELETE이고_경로_칸만_있다() throws Exception {
        givenCodes();
        String t = tool("delete_pet");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("pet"))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void 공동양육_연동_해제_도구는_DELETE이고_경로_칸_두_개만_있다() throws Exception {
        givenCodes();
        String t = tool("unlink_caregiver");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("pet"))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/caregivers/{brupRelSn}"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.brupRelSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }

    @Test
    void 취소한_초대_삭제_도구는_DELETE이고_경로_칸만_있다() throws Exception {
        givenCodes();
        String t = tool("delete_caregiver_invitation");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("pet"))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".call.path").value("/api/caregiver-invitations/{jntBrupInvtnSn}"))
                .andExpect(jsonPath(t + ".params.jntBrupInvtnSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }
}
