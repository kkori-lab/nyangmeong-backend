package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 반려동물 목록·상세·등록·수정·대표 지정·양육자 목록·연동 해제 API]
 */
@WebMvcTest(controllers = PetController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PetService petService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401() throws Exception {
        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpCode").value(401));
    }

    @Test
    @WithMockLoginMember
    void 목록은_역할_대표_인원과_상세_항목을_돌려준다() throws Exception {
        PetSummary pet = new PetSummary();
        pet.setPetSn(10L);
        pet.setPetNm("뭉치");
        pet.setPetSeCd("PETC0001");
        pet.setVrtyDrctInptNm("믹스");
        pet.setPetGndrCd("PETC0003");
        pet.setPetBrdt(LocalDate.of(2023, 5, 1));
        pet.setPetBdwg(new BigDecimal("4.250"));
        pet.setColrCd("PETC0099");
        pet.setColrDrctInptNm("연갈색");
        pet.setAlgyCn("닭고기,연어");
        pet.setDssCn("슬개골 탈구");
        pet.setBrupRoleCd("PETC0018");
        pet.setRprsPetYn(true);
        pet.setCaregiverCount(2);
        pet.setBscCharSbstYn(true);
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet));

        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200))
                .andExpect(jsonPath("$.data[0].petSn").value(10))
                .andExpect(jsonPath("$.data[0].vrtySn").doesNotExist())
                .andExpect(jsonPath("$.data[0].vrtyDrctInptNm").value("믹스"))
                .andExpect(jsonPath("$.data[0].colrDrctInptNm").value("연갈색"))
                .andExpect(jsonPath("$.data[0].algyCn").value("닭고기,연어"))
                .andExpect(jsonPath("$.data[0].dssCn").value("슬개골 탈구"))
                .andExpect(jsonPath("$.data[0].brupRoleCd").value("PETC0018"))
                .andExpect(jsonPath("$.data[0].rprsPetYn").value(true))
                .andExpect(jsonPath("$.data[0].caregiverCount").value(2))
                .andExpect(jsonPath("$.data[0].bscCharSbstYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 상세는_모달_값을_주고_체중_잠금_필드는_없다() throws Exception {
        PetResponse res = new PetResponse(10L, "뭉치", "PETC0001", 15L, "푸들", null, "PETC0003",
                LocalDate.of(2023, 5, 1), new BigDecimal("4.250"), "PETC0006", null, null, null, null, null,
                "PETC0018", true, true, null, new BigDecimal("3"), new BigDecimal("5"), List.of());
        when(petService.getPet(1L, 10L)).thenReturn(res);

        mockMvc.perform(get("/api/pets/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vrtyNm").value("푸들"))
                .andExpect(jsonPath("$.data.nutrYn").value(true))
                .andExpect(jsonPath("$.data.minBdwg").value(3))
                .andExpect(jsonPath("$.data.bdwgEditableYn").doesNotExist())
                .andExpect(jsonPath("$.data.latestBdwgRcdDt").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 등록은_201() throws Exception {
        when(petService.createPet(eq(1L), any())).thenReturn(null);

        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "4.25", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201));
    }

    @Test
    @WithMockLoginMember
    void 이름이_21자면_400() throws Exception {
        mockMvc.perform(json(post("/api/pets"), createBody("가".repeat(21), "4.25", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(petService, never()).createPet(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 체중이_0이거나_소수_넷째_자리면_400() throws Exception {
        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "0", "")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "4.2501", "")))
                .andExpect(status().isBadRequest());
        verify(petService, never()).createPet(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 칩_번호에_문자가_섞이면_400() throws Exception {
        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "4.25", "41000A")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(patch("/api/pets/10"), "{\"chipNo\":\"41000A\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 수정은_200() throws Exception {
        when(petService.updatePet(eq(1L), eq(10L), any())).thenReturn(null);

        mockMvc.perform(json(patch("/api/pets/10"), "{\"petBdwg\":6.5,\"chipNo\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
    }

    @Test
    @WithMockLoginMember
    void 대표_지정은_200() throws Exception {
        mockMvc.perform(post("/api/pets/10/representative").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
        verify(petService).designateRepresentative(1L, 10L);
    }

    @Test
    @WithMockLoginMember
    void 양육자_목록은_역할과_연동일을_돌려준다() throws Exception {
        CaregiverSummary primary = new CaregiverSummary();
        primary.setBrupRelSn(5L);
        primary.setMbrSn(9L);
        primary.setMbrNcnm("김주인");
        primary.setBrupRoleCd("PETC0018");
        primary.setLinkYmd(LocalDate.of(2026, 6, 1));
        CaregiverSummary me = new CaregiverSummary();
        me.setBrupRelSn(6L);
        me.setMbrSn(1L);
        me.setMbrNcnm("이서연");
        me.setBrupRoleCd("PETC0019");
        me.setLinkYmd(LocalDate.of(2026, 7, 2));
        when(petService.searchCaregivers(1L, 10L)).thenReturn(List.of(primary, me));

        mockMvc.perform(get("/api/pets/10/caregivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brupRoleCd").value("PETC0018"))
                .andExpect(jsonPath("$.data[0].mbrNcnm").value("김주인"))
                .andExpect(jsonPath("$.data[1].brupRelSn").value(6))
                .andExpect(jsonPath("$.data[1].linkYmd").value("2026-07-02"));
    }

    @Test
    @WithMockLoginMember
    void 양육관계가_없는_반려동물의_양육자_목록은_403() throws Exception {
        when(petService.searchCaregivers(1L, 10L)).thenThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED));

        mockMvc.perform(get("/api/pets/10/caregivers"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.httpCode").value(403));
    }

    @Test
    @WithMockLoginMember
    void 연동_해제는_200() throws Exception {
        mockMvc.perform(delete("/api/pets/10/caregivers/6").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
        verify(petService).unlinkCaregiver(1L, 10L, 6L);
    }

    @Test
    @WithMockLoginMember
    void 없는_연동의_해제는_404() throws Exception {
        doThrow(new CustomException(ErrorCode.CAREGIVER_NOT_FOUND)).when(petService).unlinkCaregiver(1L, 10L, 6L);

        mockMvc.perform(delete("/api/pets/10/caregivers/6").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpCode").value(404));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String createBody(String petNm, String petBdwg, String chipNo) {
        return """
                {"petNm":"%s","petSeCd":"PETC0001","vrtySn":15,"petBrdt":"2023-05-01","chipNo":"%s",
                 "petGndrCd":"PETC0003","nutrYn":true,"petBdwg":%s,"minBdwg":3,"maxBdwg":5,"colrCd":"PETC0006"}
                """.formatted(petNm, chipNo, petBdwg);
    }
}
