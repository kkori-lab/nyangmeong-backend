package com.nyangmeong.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.dto.CareRecruitmentCreateRequest;
import com.nyangmeong.community.dto.CareRecruitmentResponse;
import com.nyangmeong.community.dto.CareRecruitmentSearchCond;
import com.nyangmeong.community.dto.CareRecruitmentSummary;
import com.nyangmeong.community.dto.CareRecruitmentUpdateRequest;
import com.nyangmeong.community.service.CareRecruitmentService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 우리동네 펫돌봄 API]
 */
@WebMvcTest(controllers = CareRecruitmentController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CareRecruitmentControllerTest {

    private static final String AJAX = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CareRecruitmentService careRecruitmentService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private CareRecruitmentResponse response() {
        CareRecruitmentDetail detail = new CareRecruitmentDetail();
        detail.setPetPrtcRcrtSn(5L);
        detail.setPstSn(7L);
        detail.setMbrSn(1L);
        detail.setMbrNcnm("토리맘");
        detail.setMbrAtchFileSn(25L);
        detail.setPstTtl("산책 도와주실 분");
        detail.setRcrtSttsCd("PCAC0001");
        detail.setCstUnitCd("PCAC0004");
        detail.setCstAmt(15000);
        detail.setSchdlCnsltnPsbltyYn("N");
        detail.setPrtcBgngDt(LocalDateTime.of(2026, 11, 10, 19, 0));
        detail.setPrtcPlcDaddrEnc("enc");
        return CareRecruitmentResponse.of(detail, List.of(), List.of("PCAC0008"), List.of(), false, true);
    }

    /** 등록 공통 필수 항목, 반려동물은 인덱스 필드로 보낸다 */
    private MockMultipartHttpServletRequestBuilder withForm(MockMultipartHttpServletRequestBuilder builder) {
        return (MockMultipartHttpServletRequestBuilder) builder
                .param("pets[0].petSeCd", "PETC0001").param("pets[0].vrtySn", "1").param("pets[0].petSizeCd", "SMALL")
                .param("pets[1].petSeCd", "PETC0002").param("pets[1].vrtyDrctInptNm", "코숏")
                .param("prtcShpCds", "PCAC0008", "PCAC0009")
                .param("prtcBgngDt", "2026-11-10T19:00:00").param("prtcEndDt", "2026-11-10T19:30:00")
                .param("schdlCnsltnPsbltyYn", "false").param("cstUnitCd", "PCAC0004").param("cstAmt", "10320")
                .param("rlsRgnNm", "전주시 완산구 서곡동").param("prtcPlcDaddr", "서곡동 12-3")
                .param("prtcPlcLat", "35.8242").param("prtcPlcLot", "127.1480")
                .param("pstTtl", "산책 도와주실 분").param("pstCn", "화·목 저녁").param("utztnRulAgreYn", "true")
                .header("X-Requested-With", AJAX);
    }

    @Test
    @WithMockLoginMember
    void 목록은_조건을_서비스에_넘긴다() throws Exception {
        CareRecruitmentSummary row = new CareRecruitmentSummary();
        row.setPetPrtcRcrtSn(5L);
        row.setMbrNcnm("토리맘");
        row.setMbrAtchFileSn(25L);
        row.setAplcntCnt(3);
        row.setOwner(true);
        when(careRecruitmentService.search(eq(1L), any(CareRecruitmentSearchCond.class), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 10, 1, 1, false));

        mockMvc.perform(get("/api/care-recruitments").param("rcrtSttsCd", "PCAC0001").param("rgnSn", "3")
                .param("prtcBgngDe", "2026-11-10").param("prtcEndDe", "2026-11-12").param("petSeCd", "PETC0001")
                .param("petSizeCd", "SMALL").param("mineYn", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].mbrNcnm").value("토리맘"))
                .andExpect(jsonPath("$.data.content[0].mbrAtchFileSn").value(25))
                .andExpect(jsonPath("$.data.content[0].aplcntCnt").value(3));

        ArgumentCaptor<CareRecruitmentSearchCond> cond = ArgumentCaptor.forClass(CareRecruitmentSearchCond.class);
        verify(careRecruitmentService).search(eq(1L), cond.capture(), any());
        assertThat(cond.getValue().isMineYn()).isTrue();
        assertThat(cond.getValue().getRgnSn()).isEqualTo(3L);
        assertThat(cond.getValue().getPrtcBgngDe()).hasToString("2026-11-10");
        assertThat(cond.getValue().getPetSizeCd()).isEqualTo("SMALL");
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/care-recruitments")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 상세_응답에는_상세_주소와_좌표가_없다() throws Exception {
        when(careRecruitmentService.getRecruitment(1L, 5L)).thenReturn(response());

        mockMvc.perform(get("/api/care-recruitments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mbrAtchFileSn").value(25))
                .andExpect(jsonPath("$.data.owner").value(true))
                .andExpect(jsonPath("$.data.prtcPlcDaddr").doesNotExist())
                .andExpect(jsonPath("$.data.prtcPlcDaddrEnc").doesNotExist())
                .andExpect(jsonPath("$.data.prtcPlcLat").doesNotExist())
                .andExpect(jsonPath("$.data.prtcPlcLot").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 없는_모집은_404() throws Exception {
        when(careRecruitmentService.getRecruitment(1L, 9L))
                .thenThrow(new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND));

        mockMvc.perform(get("/api/care-recruitments/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARE_RECRUITMENT_NOT_FOUND"));
    }

    @Test
    @WithMockLoginMember
    void 등록은_multipart로_받아_인덱스_필드를_목록으로_묶고_201() throws Exception {
        when(careRecruitmentService.create(eq(1L), any())).thenReturn(response());

        mockMvc.perform(withForm(multipart("/api/care-recruitments")
                .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1}))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.petPrtcRcrtSn").value(5));

        ArgumentCaptor<CareRecruitmentCreateRequest> req = ArgumentCaptor.forClass(CareRecruitmentCreateRequest.class);
        verify(careRecruitmentService).create(eq(1L), req.capture());
        assertThat(req.getValue().pets()).hasSize(2);
        assertThat(req.getValue().pets().get(0).petSizeCd()).isEqualTo("SMALL");
        assertThat(req.getValue().pets().get(1).vrtyDrctInptNm()).isEqualTo("코숏");
        assertThat(req.getValue().prtcShpCds()).containsExactly("PCAC0008", "PCAC0009");
        assertThat(req.getValue().prtcBgngDt()).isEqualTo(LocalDateTime.of(2026, 11, 10, 19, 0));
        assertThat(req.getValue().images()).hasSize(1);
    }

    @Test
    @WithMockLoginMember
    void 등록_필수_항목이_빠지거나_길이를_넘으면_400() throws Exception {
        mockMvc.perform(multipart("/api/care-recruitments").param("cstUnitCd", "PCAC0004")
                .header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/care-recruitments")).param("pstTtl", "가".repeat(51)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/care-recruitments")).param("pstCn", "가".repeat(2001)))
                .andExpect(status().isBadRequest());
        /** 같은 이름의 값이 둘이면 쉼표로 이어 붙으므로 값을 덮어써서 보낸다 */
        mockMvc.perform(withForm(multipart("/api/care-recruitments")).with(request -> {
            request.setParameter("pstTtl", "   ");
            return request;
        })).andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/care-recruitments")).with(request -> {
            request.setParameter("cstAmt", "0");
            return request;
        })).andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/care-recruitments")).param("pets[2].petSeCd", "PETC0001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 최저시급_미만은_서비스_오류를_400으로_돌려준다() throws Exception {
        when(careRecruitmentService.create(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.CARE_MIN_WAGE_NOT_MET));

        mockMvc.perform(withForm(multipart("/api/care-recruitments")).with(request -> {
            request.setParameter("cstAmt", "10319");
            return request;
        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CARE_MIN_WAGE_NOT_MET"));
    }

    @Test
    @WithMockLoginMember
    void 수정은_PATCH_multipart로_받는다() throws Exception {
        when(careRecruitmentService.update(eq(1L), eq(5L), any())).thenReturn(response());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/care-recruitments/5").param("cstAmt", "12000")
                .param("keepAtchFileSns", "31", "32").header("X-Requested-With", AJAX))
                .andExpect(status().isOk());

        verify(careRecruitmentService).update(eq(1L), eq(5L), argThat((CareRecruitmentUpdateRequest r) ->
                r.cstAmt() == 12000 && r.keepAtchFileSns().equals(List.of(31L, 32L)) && r.pets() == null));
    }

    @Test
    @WithMockLoginMember
    void 삭제는_data_없이_200() throws Exception {
        mockMvc.perform(delete("/api/care-recruitments/5").header("X-Requested-With", AJAX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(careRecruitmentService).delete(1L, 5L);
    }
}
