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
import com.nyangmeong.pet.service.BreedService;
import com.nyangmeong.pet.service.CaregiverInvitationService;
import com.nyangmeong.pet.service.PetService;
import com.nyangmeong.pet.service.PetShortcutService;
import com.nyangmeong.pet.service.PointService;

/**
 * [검사 - 반려동물 도메인 AI 도구 목록 추가분]
 * - 사이트 기능 전체 표의 읽기 후보에 붙인 @AiTool이 실리는지 (READ 5)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, BreedController.class, CaregiverInvitationController.class,
        PetController.class, PetShortcutController.class, PointLedgerController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class PetReadAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    BreedService breedService;
    @MockitoBean
    CaregiverInvitationService caregiverInvitationService;
    @MockitoBean
    PetService petService;
    @MockitoBean
    PetShortcutService petShortcutService;
    @MockitoBean
    PointService pointService;

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
    void 새_도구가_종류와_도메인과_함께_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("list_breeds") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_breeds") + ".domain").value("pet"))
                .andExpect(jsonPath(tool("get_caregiver_invitation") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_caregiver_invitation") + ".domain").value("pet"))
                .andExpect(jsonPath(tool("get_pet_limit") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_pet_limit") + ".domain").value("pet"))
                .andExpect(jsonPath(tool("list_main_shortcuts") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_main_shortcuts") + ".domain").value("pet"))
                .andExpect(jsonPath(tool("list_point_ledger_months") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_point_ledger_months") + ".domain").value("pet"));
    }

    @Test
    void list_breeds_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_breeds");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/breeds"))
                .andExpect(jsonPath(t + ".params.petSeCd.in").value("query"))
                .andExpect(jsonPath(t + ".params.petSeCd.required").value(true))
                .andExpect(jsonPath(t + ".params.petSeCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.petSeCd.group").value("PETG01"))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void get_caregiver_invitation_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_caregiver_invitation");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/caregiver-invitations/{jntBrupInvtnSn}"))
                .andExpect(jsonPath(t + ".params.jntBrupInvtnSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.jntBrupInvtnSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void get_pet_limit_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_pet_limit");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/limit"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void list_main_shortcuts_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_main_shortcuts");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/main-shortcuts"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void list_point_ledger_months_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_point_ledger_months");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/point-ledgers/months"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }
}
