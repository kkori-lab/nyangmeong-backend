package com.nyangmeong.pet.controller;

import static org.hamcrest.Matchers.hasItems;
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
 * [검사 - 반려동물 쓰기 AI 카드 도구]
 * - draft_pet : 칸·필수·코드·조건부 규칙
 * - 공동양육 초대·착용·구매 : 번호 목록 칸은 array + itemType
 * - set_representative_pet·reject·cancel : 본문 없이 경로 칸만
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, PetController.class, CaregiverInvitationController.class,
        CareMissionController.class, PointController.class, PointLedgerController.class,
        PixelCharacterController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class PetWriteCardAiToolCatalogTest {

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
    void 새_도구_8개가_반려동물_CARD로_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'pet' && @.kind == 'CARD')].name",
                        hasItems("draft_pet", "set_representative_pet", "draft_caregiver_invitation",
                                "accept_caregiver_invitation", "reject_caregiver_invitation",
                                "cancel_caregiver_invitation", "draft_equipped_items", "draft_item_purchase")));
    }

    @Test
    void draft_pet_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("draft_pet");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets"))
                .andExpect(jsonPath(t + ".params.petNm.required").value(true))
                .andExpect(jsonPath(t + ".params.petNm.maxLength").value(20))
                .andExpect(jsonPath(t + ".params.petSeCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.petSeCd.group").value("PETG01"))
                .andExpect(jsonPath(t + ".params.petSeCd.required").value(true))
                .andExpect(jsonPath(t + ".params.petGndrCd.group").value("PETG02"))
                .andExpect(jsonPath(t + ".params.colrCd.group").value("PETG03"))
                .andExpect(jsonPath(t + ".params.colrCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.petBrdt.type").value("date"))
                .andExpect(jsonPath(t + ".params.petBrdt.required").value(true))
                .andExpect(jsonPath(t + ".params.nutrYn.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.petBdwg.type").value("decimal"))
                .andExpect(jsonPath(t + ".params.petBdwg.required").value(true))
                .andExpect(jsonPath(t + ".params.petBdwg.scale").value(2))
                .andExpect(jsonPath(t + ".params.vrtySn.type").value("long"))
                .andExpect(jsonPath(t + ".params.bdwgRngDrctInptYn.pattern").value("^[YN]$"))
                .andExpect(jsonPath(t + ".params.dssCn.maxLength").value(1000))
                .andExpect(jsonPath(t + ".params.*", hasSize(18)));
    }

    @Test
    void draft_pet_조건부_규칙() throws Exception {
        givenCodes();
        String t = tool("draft_pet");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.field").value("colrDrctInptNm"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.when").value("colrCd"))
                .andExpect(jsonPath(t + ".rules[0].requiredIf.equals").value("PETC0017"))
                .andExpect(jsonPath(t + ".rules[1].requiredIf.field").value("minBdwg"))
                .andExpect(jsonPath(t + ".rules[1].requiredIf.when").value("bdwgRngDrctInptYn"))
                .andExpect(jsonPath(t + ".rules[1].requiredIf.equals").value("Y"))
                .andExpect(jsonPath(t + ".rules[2].requiredIf.field").value("maxBdwg"))
                .andExpect(jsonPath(t + ".rules[3].oneOf[0]").value("vrtySn"))
                .andExpect(jsonPath(t + ".rules[3].oneOf[1]").value("vrtyDrctInptNm"));
    }

    @Test
    void set_representative_pet_경로_칸만() throws Exception {
        givenCodes();
        String t = tool("set_representative_pet");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pets/{petSn}/representative"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void draft_caregiver_invitation_이메일과_반려동물_번호_목록() throws Exception {
        givenCodes();
        String t = tool("draft_caregiver_invitation");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/caregiver-invitations"))
                .andExpect(jsonPath(t + ".params.invtnTrgtEml.required").value(true))
                .andExpect(jsonPath(t + ".params.invtnTrgtEml.maxLength").value(254))
                .andExpect(jsonPath(t + ".params.petSns.type").value("array"))
                .andExpect(jsonPath(t + ".params.petSns.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.petSns.required").value(true))
                .andExpect(jsonPath(t + ".params.petSns.minItems").value(1))
                .andExpect(jsonPath(t + ".params.petSns.items").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }

    @Test
    void accept_caregiver_invitation_경로와_반려동물_번호_목록() throws Exception {
        givenCodes();
        String t = tool("accept_caregiver_invitation");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/caregiver-invitations/{jntBrupInvtnSn}/accept"))
                .andExpect(jsonPath(t + ".params.jntBrupInvtnSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSns.type").value("array"))
                .andExpect(jsonPath(t + ".params.petSns.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.petSns.minItems").value(1))
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }

    @Test
    void 초대_거절과_취소는_경로_칸만() throws Exception {
        givenCodes();
        String reject = tool("reject_caregiver_invitation");
        String cancel = tool("cancel_caregiver_invitation");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(reject + ".call.method").value("POST"))
                .andExpect(jsonPath(reject + ".call.path").value("/api/caregiver-invitations/{jntBrupInvtnSn}/reject"))
                .andExpect(jsonPath(reject + ".params.*", hasSize(1)))
                .andExpect(jsonPath(cancel + ".call.path").value("/api/caregiver-invitations/{jntBrupInvtnSn}/cancel"))
                .andExpect(jsonPath(cancel + ".params.*", hasSize(1)));
    }

    @Test
    void draft_equipped_items_빈_목록_허용() throws Exception {
        givenCodes();
        String t = tool("draft_equipped_items");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("PUT"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pixel-characters/{pixelCharSn}/equipped-items"))
                .andExpect(jsonPath(t + ".params.hldItemSns.type").value("array"))
                .andExpect(jsonPath(t + ".params.hldItemSns.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.hldItemSns.required").value(true))
                .andExpect(jsonPath(t + ".params.hldItemSns.minItems").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }

    @Test
    void draft_item_purchase_중복_방지_키는_실행하는_쪽이_만든다() throws Exception {
        givenCodes();
        String t = tool("draft_item_purchase");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/pixel-characters/{pixelCharSn}/held-items"))
                .andExpect(jsonPath(t + ".params.idmptDmndId.generated").value("uuid"))
                .andExpect(jsonPath(t + ".params.idmptDmndId.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.itemSns.type").value("array"))
                .andExpect(jsonPath(t + ".params.itemSns.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.itemSns.minItems").value(1))
                .andExpect(jsonPath(t + ".params.*", hasSize(3)));
    }
}
