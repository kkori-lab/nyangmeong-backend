package com.nyangmeong.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.dto.LostReportResponse;
import com.nyangmeong.community.dto.LostReportSearchCond;
import com.nyangmeong.community.dto.LostReportSummary;
import com.nyangmeong.community.service.LostReportService;
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
 * [검사 - 실종센터 API]
 */
@WebMvcTest(controllers = LostReportController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class LostReportControllerTest {

    private static final String AJAX = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LostReportService lostReportService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private LostReportResponse response() {
        LostReportDetail detail = new LostReportDetail();
        detail.setLostTpofSn(5L);
        detail.setPstSn(7L);
        detail.setPstTtl("보리 (말티즈)를 찾습니다");
        detail.setLostTpofSttsCd("CMUC0006");
        detail.setPetNm("보리");
        detail.setWtnsPlcLat(new BigDecimal("35.8"));
        detail.setWtnsPlcLot(new BigDecimal("127.1"));
        detail.setExpryCrtrDt(LocalDateTime.of(2026, 11, 28, 9, 0));
        return LostReportResponse.of(detail, "010-1234-5678", List.of(), false, true);
    }

    /** 등록·수정 공통 필수 항목 */
    private MockMultipartHttpServletRequestBuilder withForm(MockMultipartHttpServletRequestBuilder builder) {
        return (MockMultipartHttpServletRequestBuilder) builder
                .param("petSeCd", "PETC0001").param("petNm", "보리").param("vrtySn", "1")
                .param("colrNm", "흰색").param("lastWtnsDt", "2026-10-29T17:00:00")
                .param("wtnsPlcLat", "35.8242").param("wtnsPlcLot", "127.1480")
                .param("cnpl", "010-1234-5678").param("pstTtl", "보리를 찾습니다").param("pstCn", "서곡동 인근")
                .param("prvcRlsAgreYn", "true").header("X-Requested-With", AJAX);
    }

    @Test
    @WithMockLoginMember
    void 목록은_조건을_서비스에_넘긴다() throws Exception {
        LostReportSummary row = new LostReportSummary();
        row.setLostTpofSn(5L);
        row.setLostTpofSttsCd("CMUC0006");
        row.setOwner(true);
        when(lostReportService.search(eq(1L), any(LostReportSearchCond.class), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 12, 1, 1, false));

        mockMvc.perform(get("/api/lost-reports").param("lostTpofSttsCd", "CMUC0006").param("petSeCd", "PETC0001")
                .param("wtnsPrd", "WEEK").param("keyword", "말티즈"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lostTpofSn").value(5))
                .andExpect(jsonPath("$.data.content[0].owner").value(true));
    }

    @Test
    @WithMockLoginMember
    void 내_글만_보기_mineYn이_조건에_바인딩된다() throws Exception {
        when(lostReportService.search(eq(1L), argThat(c -> c.isMineYn()), any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 12, 0, 0, false));

        mockMvc.perform(get("/api/lost-reports").param("mineYn", "true").param("lostTpofSttsCd", "CMUC0008"))
                .andExpect(status().isOk());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/lost-reports")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 상세는_연락처와_작성자_여부를_돌려준다() throws Exception {
        when(lostReportService.getReport(1L, 5L)).thenReturn(response());

        mockMvc.perform(get("/api/lost-reports/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cnpl").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.owner").value(true));
    }

    @Test
    @WithMockLoginMember
    void 없는_제보는_404() throws Exception {
        when(lostReportService.getReport(1L, 9L)).thenThrow(new CustomException(ErrorCode.LOST_REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/lost-reports/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOST_REPORT_NOT_FOUND"));
    }

    @Test
    @WithMockLoginMember
    void 등록은_multipart로_받아_201() throws Exception {
        when(lostReportService.create(eq(1L), any())).thenReturn(response());

        mockMvc.perform(withForm(multipart("/api/lost-reports")
                .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1}))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lostTpofSn").value(5));
    }

    @Test
    @WithMockLoginMember
    void 등록_필수_항목이_빠지거나_길이를_넘으면_400() throws Exception {
        mockMvc.perform(multipart("/api/lost-reports").param("petSeCd", "PETC0001")
                .header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/lost-reports")).param("petNm", "가".repeat(21)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/lost-reports")).param("pstTtl", "가".repeat(51)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withForm(multipart("/api/lost-reports")).param("cnpl", "전화번호"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 수정은_200() throws Exception {
        when(lostReportService.update(eq(1L), eq(5L), any())).thenReturn(response());

        mockMvc.perform(withForm(multipart(HttpMethod.PATCH, "/api/lost-reports/5")).param("keepAtchFileSns", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lostTpofSn").value(5));
    }

    @Test
    @WithMockLoginMember
    void 남의_제보_수정은_403() throws Exception {
        when(lostReportService.update(eq(1L), eq(5L), any())).thenThrow(new CustomException(ErrorCode.NOT_OWNER));

        mockMvc.perform(withForm(multipart(HttpMethod.PATCH, "/api/lost-reports/5")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));
    }

    @Test
    @WithMockLoginMember
    void 삭제는_200이고_data가_없다() throws Exception {
        mockMvc.perform(delete("/api/lost-reports/5").header("X-Requested-With", AJAX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(lostReportService).delete(1L, 5L);
    }

    @Test
    @WithMockLoginMember
    void 찾았어요와_연장은_200() throws Exception {
        mockMvc.perform(post("/api/lost-reports/5/found").header("X-Requested-With", AJAX))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/lost-reports/5/renew").header("X-Requested-With", AJAX))
                .andExpect(status().isOk());

        verify(lostReportService).markFound(1L, 5L);
        verify(lostReportService).renew(1L, 5L);
    }

    @Test
    @WithMockLoginMember
    void 연장_창이_아니면_400() throws Exception {
        org.mockito.Mockito.doThrow(new CustomException(ErrorCode.LOST_REPORT_NOT_RENEWABLE))
                .when(lostReportService).renew(1L, 5L);

        mockMvc.perform(post("/api/lost-reports/5/renew").header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOST_REPORT_NOT_RENEWABLE"));
    }
}
