package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import com.nyangmeong.pet.dto.CaregiverInvitationAcceptRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationConflictResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateResult;
import com.nyangmeong.pet.dto.CaregiverInvitationPetSummary;
import com.nyangmeong.pet.dto.CaregiverInvitationResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationSummary;
import com.nyangmeong.pet.service.CaregiverInvitationService;

/**
 * [검사 - 공동양육 초대 발송·보낸 초대·받은 초대·수락·거절 API]
 */
@WebMvcTest(controllers = CaregiverInvitationController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CaregiverInvitationControllerTest {

    private static final String PENDING = "PETC0020";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CaregiverInvitationService caregiverInvitationService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401() throws Exception {
        mockMvc.perform(get("/api/caregiver-invitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpCode").value(401));
    }

    @Test
    @WithMockLoginMember
    void 초대_발송은_201과_초대를_돌려준다() throws Exception {
        when(caregiverInvitationService.createInvitation(eq(1L), any())).thenReturn(
                CaregiverInvitationCreateResult.success(new CaregiverInvitationResponse(50L, "김주인", PENDING,
                        LocalDateTime.of(2026, 11, 5, 10, 0), "s***@example.com", false,
                        List.of(pet(100L, "뭉치")))));

        mockMvc.perform(json(post("/api/caregiver-invitations"),
                "{\"invtnTrgtEml\":\"sooyoung@example.com\",\"petSns\":[100]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.jntBrupInvtnSn").value(50))
                .andExpect(jsonPath("$.data.invtnExpryDt").value("2026-11-05T10:00:00"))
                .andExpect(jsonPath("$.data.pets[0].petNm").value("뭉치"));
    }

    @Test
    @WithMockLoginMember
    void 이미_가족이면_409와_반려동물명을_돌려준다() throws Exception {
        when(caregiverInvitationService.createInvitation(eq(1L), any())).thenReturn(
                CaregiverInvitationCreateResult.fail(ErrorCode.CAREGIVER_ALREADY_LINKED,
                        new CaregiverInvitationConflictResponse(101L, "나비", null)));

        mockMvc.perform(json(post("/api/caregiver-invitations"),
                "{\"invtnTrgtEml\":\"sooyoung@example.com\",\"petSns\":[100,101]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAREGIVER_ALREADY_LINKED"))
                .andExpect(jsonPath("$.data.petNm").value("나비"));
    }

    @Test
    @WithMockLoginMember
    void 대기_중인_초대가_있으면_409와_만료일시를_돌려준다() throws Exception {
        when(caregiverInvitationService.createInvitation(eq(1L), any())).thenReturn(
                CaregiverInvitationCreateResult.fail(ErrorCode.CAREGIVER_INVITATION_DUPLICATED,
                        new CaregiverInvitationConflictResponse(100L, "뭉치", LocalDateTime.of(2026, 11, 3, 9, 0))));

        mockMvc.perform(json(post("/api/caregiver-invitations"),
                "{\"invtnTrgtEml\":\"sooyoung@example.com\",\"petSns\":[100]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAREGIVER_INVITATION_DUPLICATED"))
                .andExpect(jsonPath("$.data.invtnExpryDt").value("2026-11-03T09:00:00"));
    }

    @Test
    @WithMockLoginMember
    void 이메일_형식이_틀리면_400() throws Exception {
        mockMvc.perform(json(post("/api/caregiver-invitations"), "{\"invtnTrgtEml\":\"abc\",\"petSns\":[100]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(caregiverInvitationService, never()).createInvitation(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 반려동물을_고르지_않으면_400() throws Exception {
        mockMvc.perform(json(post("/api/caregiver-invitations"),
                "{\"invtnTrgtEml\":\"sooyoung@example.com\",\"petSns\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(caregiverInvitationService, never()).createInvitation(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 내_이메일이면_400() throws Exception {
        when(caregiverInvitationService.createInvitation(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.CAREGIVER_INVITATION_SELF));

        mockMvc.perform(json(post("/api/caregiver-invitations"),
                "{\"invtnTrgtEml\":\"me@example.com\",\"petSns\":[100]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAREGIVER_INVITATION_SELF"));
    }

    @Test
    @WithMockLoginMember
    void 보낸_초대_목록은_반려동물별_상태와_가입_여부를_돌려준다() throws Exception {
        CaregiverInvitationSummary row = new CaregiverInvitationSummary();
        row.setJntBrupInvtnSn(50L);
        row.setInvtnTrgtEml("sooyoung@example.com");
        row.setRcvrJoined(false);
        row.setInvtnSttsCd(PENDING);
        CaregiverInvitationPetSummary pet = pet(100L, "뭉치");
        pet.setInvtnSttsCd(PENDING);
        row.setPets(List.of(pet));
        when(caregiverInvitationService.searchSentInvitations(1L)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/caregiver-invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].invtnTrgtEml").value("sooyoung@example.com"))
                .andExpect(jsonPath("$.data[0].rcvrJoined").value(false))
                .andExpect(jsonPath("$.data[0].pets[0].invtnSttsCd").value(PENDING));
    }

    @Test
    @WithMockLoginMember
    void 다른_계정의_초대_상세는_가린_이메일만_돌려준다() throws Exception {
        when(caregiverInvitationService.getInvitation(1L, 50L)).thenReturn(new CaregiverInvitationResponse(50L,
                null, PENDING, LocalDateTime.of(2026, 11, 5, 10, 0), "s***@example.com", false, null));

        mockMvc.perform(get("/api/caregiver-invitations/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailMatched").value(false))
                .andExpect(jsonPath("$.data.invtnTrgtEmlMask").value("s***@example.com"))
                .andExpect(jsonPath("$.data.pets").doesNotExist())
                .andExpect(jsonPath("$.data.sndrMbrNcnm").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 없는_초대는_404() throws Exception {
        when(caregiverInvitationService.getInvitation(1L, 50L))
                .thenThrow(new CustomException(ErrorCode.CAREGIVER_INVITATION_NOT_FOUND));

        mockMvc.perform(get("/api/caregiver-invitations/50"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpCode").value(404));
    }

    @Test
    @WithMockLoginMember
    void 수락은_200() throws Exception {
        mockMvc.perform(json(post("/api/caregiver-invitations/50/accept"), "{\"petSns\":[100]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
        verify(caregiverInvitationService).acceptInvitation(1L, 50L,
                new CaregiverInvitationAcceptRequest(List.of(100L)));
    }

    @Test
    @WithMockLoginMember
    void 수락할_반려동물을_고르지_않으면_400() throws Exception {
        mockMvc.perform(json(post("/api/caregiver-invitations/50/accept"), "{\"petSns\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(caregiverInvitationService, never()).acceptInvitation(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 다른_이메일_계정의_수락은_403() throws Exception {
        doThrow(new CustomException(ErrorCode.CAREGIVER_INVITATION_EMAIL_MISMATCH)).when(caregiverInvitationService)
                .acceptInvitation(eq(1L), eq(50L), any());

        mockMvc.perform(json(post("/api/caregiver-invitations/50/accept"), "{\"petSns\":[100]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAREGIVER_INVITATION_EMAIL_MISMATCH"));
    }

    @Test
    @WithMockLoginMember
    void 만료된_초대의_수락은_400() throws Exception {
        doThrow(new CustomException(ErrorCode.CAREGIVER_INVITATION_EXPIRED)).when(caregiverInvitationService)
                .acceptInvitation(eq(1L), eq(50L), any());

        mockMvc.perform(json(post("/api/caregiver-invitations/50/accept"), "{\"petSns\":[100]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAREGIVER_INVITATION_EXPIRED"));
    }

    @Test
    @WithMockLoginMember
    void 거절은_200() throws Exception {
        mockMvc.perform(post("/api/caregiver-invitations/50/reject").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
        verify(caregiverInvitationService).rejectInvitation(1L, 50L);
    }

    @Test
    @WithMockLoginMember
    void 이미_처리된_초대의_거절은_409() throws Exception {
        doThrow(new CustomException(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED))
                .when(caregiverInvitationService).rejectInvitation(1L, 50L);

        mockMvc.perform(post("/api/caregiver-invitations/50/reject").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAREGIVER_INVITATION_ALREADY_PROCESSED"));
    }

    private CaregiverInvitationPetSummary pet(Long petSn, String petNm) {
        CaregiverInvitationPetSummary pet = new CaregiverInvitationPetSummary();
        pet.setPetSn(petSn);
        pet.setPetNm(petNm);
        pet.setPetSeCd("PETC0001");
        return pet;
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
