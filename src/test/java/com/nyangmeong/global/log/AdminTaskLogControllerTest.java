package com.nyangmeong.global.log;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.GlobalExceptionHandler;
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
 * [검사 - 관리자 감사 로그 API]
 */
@WebMvcTest(controllers = AdminTaskLogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminTaskLogControllerTest {

    private static final String ADMIN = "USRC0005";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminTaskLogService adminTaskLogService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_조건으로_목록을_조회한다() throws Exception {
        AdminTaskLogSummary row = new AdminTaskLogSummary();
        row.setMngrTaskLogSn(1L);
        row.setMbrNcnm("관리자01");
        when(adminTaskLogService.search(any(), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 20, 1L, 1, false));

        mockMvc.perform(get("/api/admin/audit-logs").param("mbrSn", "9").param("taskTypeCd", "SYSC0002")
                .param("trgtTblNm", "SYS_STNG").param("bgngYmd", "2026-10-01").param("endYmd", "2026-10-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].mbrNcnm").value("관리자01"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(adminTaskLogService).search(argThat(cond -> cond.getMbrSn() == 9L
                && "SYSC0002".equals(cond.getTaskTypeCd()) && "SYS_STNG".equals(cond.getTrgtTblNm())
                && LocalDate.of(2026, 10, 1).equals(cond.getBgngYmd())
                && LocalDate.of(2026, 10, 30).equals(cond.getEndYmd())), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자_필터_목록을_조회한다() throws Exception {
        AdminTaskLogAdminSummary admin = new AdminTaskLogAdminSummary();
        admin.setMbrSn(9L);
        admin.setMbrNcnm("관리자01");
        when(adminTaskLogService.searchAdmins()).thenReturn(List.of(admin));

        mockMvc.perform(get("/api/admin/audit-logs/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mbrSn").value(9));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 상세를_조회한다() throws Exception {
        when(adminTaskLogService.getDetail(eq(1L))).thenReturn(new AdminTaskLogDetailResponse(1L, null, 9L, "관리자01",
                "SYSC0002", "SYS_STNG", 1L, "운영 설정 수정", null, null, "203.0.113.7"));

        mockMvc.perform(get("/api/admin/audit-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cntnIpAddr").value("203.0.113.7"));
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    void 비로그인은_401() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 수정_API는_없다() throws Exception {
        mockMvc.perform(patch("/api/admin/audit-logs/1").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 삭제_API는_없다() throws Exception {
        mockMvc.perform(delete("/api/admin/audit-logs/1").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isMethodNotAllowed());
    }
}
