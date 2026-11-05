package com.nyangmeong.pet.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.nyangmeong.pet.service.CareMissionService;
import com.nyangmeong.pet.service.CaregiverInvitationService;
import com.nyangmeong.pet.service.ItemService;
import com.nyangmeong.pet.service.PetService;
import com.nyangmeong.pet.service.PixelCharacterService;
import com.nyangmeong.pet.service.PointService;

/**
 * [검사 - 반려동물 도메인 AI 도구 목록]
 * - 실제 반려동물 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 10)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, PetController.class, CaregiverInvitationController.class,
        CareMissionController.class, PointController.class, PointLedgerController.class,
        PixelCharacterController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class PetAiToolCatalogTest {

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
    @MockitoBean
    CareMissionService careMissionService;
    @MockitoBean
    PointService pointService;
    @MockitoBean
    PixelCharacterService pixelCharacterService;
    @MockitoBean
    ItemService itemService;

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
    void 반려동물_READ_10개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(10))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("get_pet", "get_pixel_character",
                        "get_points", "list_caregivers", "list_daily_missions", "list_pets", "list_pixel_characters",
                        "list_point_ledgers", "list_sent_caregiver_invitations", "list_shop_items")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(10)))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'pet')].name", hasSize(10)));
    }

    @Test
    void 포인트_내역_도구의_연월은_문자열과_형식으로_실린다() throws Exception {
        givenCodes();
        String t = tool("list_point_ledgers");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/point-ledgers"))
                .andExpect(jsonPath(t + ".params.yearMonth.in").value("query"))
                .andExpect(jsonPath(t + ".params.yearMonth.type").value("string"))
                .andExpect(jsonPath(t + ".params.yearMonth.pattern").value("^\\d{4}-(0[1-9]|1[0-2])$"))
                .andExpect(jsonPath(t + ".params.yearMonth.required").doesNotExist());
    }

    @Test
    void 상점_도구는_캐릭터가_경로이고_부위가_코드다() throws Exception {
        givenCodes();
        String t = tool("list_shop_items");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/pixel-characters/{pixelCharSn}/shop-items"))
                .andExpect(jsonPath(t + ".params.pixelCharSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.itemRegnCd.in").value("query"))
                .andExpect(jsonPath(t + ".params.itemRegnCd.group").value("MYPG01"))
                .andExpect(jsonPath(t + ".params.itemRegnCd.required").doesNotExist());
    }

    @Test
    void 오늘의_미션_도구는_기준일이_날짜다() throws Exception {
        givenCodes();
        String t = tool("list_daily_missions");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.msnCrtrYmd.type").value("date"));
    }
}
